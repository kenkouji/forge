import uuid
import logging
import os
import json
import time
from typing import Dict, Any, Optional
from sqlalchemy.orm import Session
from app.ai.provider import get_ai_provider
from app.ai.schemas import ChatRequest, ChatResponse, ToolCallPayload
from app.models.entities import Automation
from app.permissions.registry import validate_tool_and_permissions, is_tool_registered
from app.websocket.manager import ws_manager
from app.audit.logger import record_audit_log

logger = logging.getLogger("remoteforge.ai.engine")

class AIEngine:
    def __init__(self):
        self.provider = get_ai_provider()

    async def handle_user_message(
        self,
        request: ChatRequest,
        db: Session,
        actor: str = "admin"
    ) -> ChatResponse:
        device_id = request.device_id

        # If user is confirming a pending action
        if request.confirmed and request.pending_tool:
            tool_name = request.pending_tool
            tool_args = request.pending_arguments or {}
            return await self._execute_tool_workflow(
                tool_name=tool_name,
                tool_args=tool_args,
                device_id=device_id,
                db=db,
                actor=actor,
                is_confirmed=True
            )

        # Process user message with provider
        initial_reply, tool_call = await self.provider.process_message(request.message, history=[])

        if not tool_call:
            return ChatResponse(reply=initial_reply)

        tool_name = tool_call.tool
        tool_args = tool_call.arguments

        # Security check: verify tool is strictly registered
        if not is_tool_registered(tool_name):
            return ChatResponse(
                reply=f"Security Policy: Tool '{tool_name}' is not permitted.",
                error="Tool rejected by policy enforcer."
            )

        # Check permissions & confirmation requirement
        perm_check = validate_tool_and_permissions(tool_name, tool_args, is_confirmed=request.confirmed)
        if perm_check.get("requires_confirmation") and not request.confirmed:
            return ChatResponse(
                reply=f"This high-risk action requires your explicit confirmation before proceeding.",
                tool_called=tool_name,
                tool_arguments=tool_args,
                requires_confirmation=True,
                confirmation_prompt=f"Are you sure you want to {tool_name.replace('_', ' ')}?"
            )

        if not perm_check.get("valid"):
            return ChatResponse(
                reply=f"Action blocked by policy: {perm_check.get('error', 'Unauthorized')}",
                error=perm_check.get("error")
            )

        # Execute tool workflow
        return await self._execute_tool_workflow(
            tool_name=tool_name,
            tool_args=tool_args,
            device_id=device_id,
            db=db,
            actor=actor,
            is_confirmed=request.confirmed
        )

    async def _execute_tool_workflow(
        self,
        tool_name: str,
        tool_args: Dict[str, Any],
        device_id: Optional[str],
        db: Session,
        actor: str,
        is_confirmed: bool
    ) -> ChatResponse:
        if tool_name == "schedule_task":
            name = tool_args.get("name", "Scheduled Task")
            action_tool = tool_args.get("action_tool", "open_browser")
            action_arguments = tool_args.get("action_arguments", {})
            delay_seconds = int(tool_args.get("delay_seconds", 1200))
            scheduled_epoch = time.time() + delay_seconds
            schedule_time = tool_args.get("schedule_time", f"In {delay_seconds // 60} mins")

            auto_id = str(uuid.uuid4())
            auto = Automation(
                id=auto_id,
                name=name,
                description=tool_args.get("description", f"Scheduled for {schedule_time}"),
                trigger_type="Delayed Task",
                schedule_time=schedule_time,
                action_tool=action_tool,
                action_arguments_json=json.dumps(action_arguments),
                target_device_id=device_id or "",
                status="ACTIVE",
                next_run_time=schedule_time,
                scheduled_epoch=scheduled_epoch,
                is_one_shot=True,
                execution_count=0
            )
            db.add(auto)
            db.commit()

            record_audit_log(
                db=db,
                actor=actor,
                device_id=device_id,
                action="TASK_SCHEDULED",
                tool=action_tool,
                arguments=action_arguments,
                permission_level="LOW",
                confirmation_status="AUTO_APPROVED",
                execution_status="SUCCESS",
                details=f"Scheduled '{name}' to execute in {delay_seconds // 60} minutes."
            )

            mins = delay_seconds // 60
            desc = tool_args.get("description", name)
            return ChatResponse(
                reply=f"⏰ **Task Scheduled Successfully!**\n\nIn **{mins} minutes**, I will automatically execute on your computer:\n• **Action**: {desc}\n• **Tool**: `{action_tool}`\n• **Target Computer**: {device_id or 'Primary Workstation'}\n\nYou can view and manage all scheduled tasks anytime in the Automations menu.",
                tool_called="schedule_task",
                tool_arguments=tool_args,
                tool_result={"scheduled_id": auto_id, "scheduled_epoch": scheduled_epoch, "delay_minutes": mins}
            )

        if not device_id or not ws_manager.is_agent_online(device_id):
            return ChatResponse(
                reply=f"I wanted to run `{tool_name}`, but your target computer is currently OFFLINE.",
                tool_called=tool_name,
                tool_arguments=tool_args,
                error="Target PC is offline."
            )

        req_id = str(uuid.uuid4())
        try:
            agent_res = await ws_manager.send_command_to_agent(
                device_id=device_id,
                request_id=req_id,
                tool=tool_name,
                arguments=tool_args,
                timeout_seconds=25.0
            )
        except Exception as e:
            return ChatResponse(
                reply=f"Error communicating with PC agent: {str(e)}",
                tool_called=tool_name,
                tool_arguments=tool_args,
                error=str(e)
            )

        success = agent_res.get("success", False)
        result_data = agent_res.get("result")
        error_msg = agent_res.get("error")

        # Record audit log
        record_audit_log(
            db=db,
            actor=actor,
            device_id=device_id,
            action="AI_TOOL_EXECUTION",
            tool=tool_name,
            arguments=tool_args,
            permission_level="HIGH" if is_confirmed else "LOW",
            confirmation_status="CONFIRMED" if is_confirmed else "AUTO_APPROVED",
            execution_status="SUCCESS" if success else "FAILED",
            details=f"AI Assistant invoked '{tool_name}'" + (f": {error_msg}" if error_msg else "")
        )

        if not success:
            return ChatResponse(
                reply=f"Tool execution failed: {error_msg or 'Unknown error on PC'}",
                tool_called=tool_name,
                tool_arguments=tool_args,
                tool_result=result_data,
                error=error_msg
            )

        # Synthesize friendly reply from real result
        synthesized_text = self._synthesize_tool_reply(tool_name, tool_args, result_data)

        return ChatResponse(
            reply=synthesized_text,
            tool_called=tool_name,
            tool_arguments=tool_args,
            tool_result=result_data
        )

    def _synthesize_tool_reply(self, tool: str, args: Dict[str, Any], result: Any) -> str:
        if tool == "get_system_status" and isinstance(result, dict):
            ram = result.get("ram", {})
            cpu = result.get("cpu", {})
            disk = result.get("disk", {})
            ram_pct = ram.get("percent", 0)
            ram_used = ram.get("used_gb", 0)
            ram_total = ram.get("total_gb", 0)
            cpu_pct = cpu.get("percent", 0)
            cores = cpu.get("logical_cores", 0)
            uptime = result.get("uptime_formatted", "")
            return f"Your PC is currently using {int(ram_pct)}% RAM ({ram_used:.1f} GB / {ram_total:.0f} GB) and {int(cpu_pct)}% CPU across {cores} cores. Uptime: {uptime}."

        if tool == "search_files" and isinstance(result, dict):
            matches = result.get("matches", [])
            query = result.get("query", "")
            if not matches:
                return f"I searched your allowed user folders (Documents, Downloads, Desktop, Projects) but found no files matching '{query}'."

            # Normalize matches
            for m in matches:
                fn = m.get("filename") or m.get("name") or "File"
                m["name"] = fn
                m["filename"] = fn
                ext = os.path.splitext(fn)[1].lower().replace(".", "")
                m["extension"] = ext

            # Group files by stem to detect duplicate formats (e.g. resume.pdf vs resume.docx)
            stems: Dict[str, List[Dict[str, Any]]] = {}
            for m in matches:
                stem = os.path.splitext(m["name"])[0].lower()
                stems.setdefault(stem, []).append(m)

            has_different_extensions = any(len(group) > 1 for group in stems.values())

            if has_different_extensions:
                lines = [f"I found multiple versions of '{query}' with different extensions:"]
                item_idx = 1
                for stem, group in stems.items():
                    for f in group:
                        ext_label = (f.get("extension") or "file").upper()
                        size_kb = int(f.get("size_bytes", 0) / 1024)
                        lines.append(f"{item_idx}. **{f['name']}** ({ext_label} • {size_kb} KB)")
                        item_idx += 1
                lines.append("\nWhich one would you like to view, preview, or download? You can reply with 'PDF', 'Word', or tap directly below.")
                return "\n".join(lines)

            if len(matches) == 1:
                m = matches[0]
                ext_label = (m.get("extension") or "file").upper()
                size_kb = int(m.get("size_bytes", 0) / 1024)
                return f"Found **{m['name']}** ({ext_label} • {size_kb} KB) at `{m.get('path')}`. You can preview or download it directly below."

            first_few = "\n".join([f"• **{m.get('name')}** ({int(m.get('size_bytes', 0) / 1024)} KB) - `{m.get('path')}`" for m in matches[:5]])
            return f"Found {len(matches)} files matching '{query}':\n{first_few}\n\nTap any card below to preview or download."

        if tool == "list_directory" and isinstance(result, dict):
            items = result.get("items", [])
            count = len(items)
            preview = ", ".join([it.get("name", "") for it in items[:6]])
            return f"Directory contains {count} item(s): {preview}..."

        if tool == "take_screenshot":
            return "Screenshot captured successfully from your computer screen."

        if tool == "lock_pc":
            return "Your PC user session has been locked."

        if tool == "browser_search":
            q = args.get("query", "")
            s = args.get("site", "Google")
            return f"Launched web browser searching {s.capitalize()} for '{q}'."

        if tool == "open_browser":
            u = args.get("url", "")
            return f"Opened web browser navigating to {u}."

        if tool == "open_application":
            app = args.get("app_name", "Application")
            return f"Launched application '{app}' on your computer."

        if tool == "close_application":
            app = args.get("app_name", "Application")
            return f"Requested application '{app}' to close on your computer."

        if tool == "minimize_application":
            app = args.get("app_name", "Application")
            return f"Application '{app}' has been minimized on your computer."

        if tool == "run_command":
            cmd = args.get("command") or args.get("cmd") or ""
            out = result.get("output", "") if isinstance(result, dict) else str(result)
            return f"Executed command `{cmd}`:\n```\n{out}\n```"

        if tool == "say_text":
            txt = args.get("text", "")
            return f"Spoke aloud on workstation speakers: \"{txt}\""

        if tool == "play_alert":
            return "Played alert chime sound on your workstation."

        if tool == "show_notification":
            msg = args.get("message", "")
            return f"Desktop banner notification displayed on your screen: \"{msg}\""

        if tool == "set_volume":
            lvl = args.get("level", 50)
            return f"Workstation audio volume adjusted to {lvl}%."

        if tool in ["restart_pc", "shutdown_pc"]:
            return f"Execution command for '{tool.replace('_', ' ')}' dispatched to PC agent."

        return f"Executed {tool} successfully."

ai_engine = AIEngine()
