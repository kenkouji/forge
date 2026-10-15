import os
import re
import json
import logging
from abc import ABC, abstractmethod
from typing import Dict, Any, List, Optional, Tuple
from app.ai.schemas import ToolCallPayload
from app.ai.prompts import SYSTEM_PROMPT
from app.ai.tools import AI_AVAILABLE_TOOLS

logger = logging.getLogger("remoteforge.ai.provider")

class BaseAIProvider(ABC):
    @abstractmethod
    async def process_message(
        self,
        message: str,
        history: List[Dict[str, str]]
    ) -> Tuple[str, Optional[ToolCallPayload]]:
        """
        Processes user message.
        Returns: (assistant_text, optional_tool_call)
        """
        pass

class LocalRuleAIProvider(BaseAIProvider):
    """
    Intelligent built-in provider that parses intent and generates structured tool calls
    without requiring external cloud API keys or dependencies.
    """
    async def process_message(
        self,
        message: str,
        history: List[Dict[str, str]]
    ) -> Tuple[str, Optional[ToolCallPayload]]:
        text = message.strip()
        lower = text.lower()

        # Security check: User asking to explore forbidden paths
        if any(f in lower for f in ["/etc", "/system", "/library", "/private", "c:\\windows", ".ssh", ".aws", ".env"]):
            return (
                "I cannot access system or credential directories. RemoteForge is strictly sandboxed to your configured user-data folders (Desktop, Documents, Downloads, Pictures, Projects).",
                None
            )

        # Security check: User attempting arbitrary shell commands
        if any(f in lower for f in ["bash", "sh ", "curl", "rm -rf", "sudo", "execute_shell", "powershell", "cmd.exe", "exec("]):
            return (
                "Arbitrary shell commands cannot be executed. RemoteForge strictly operates through authorized, registered tools with sandbox security.",
                None
            )

        # 0. Scheduled Task Intent (e.g. 'in 20 mins open youtube and search pewdiepie', 'in 20 mins ill reach home so open googlechrome and open the gmail')
        delay_match = re.search(r"\b(?:in\s+(?:about\s+)?(\d+)\s*(mins?|minutes?|hours?|secs?|seconds?)|after\s+(\d+)\s*(mins?|minutes?|hours?))\b", text, re.IGNORECASE)
        if delay_match:
            num = int(delay_match.group(1) or delay_match.group(3) or "20")
            unit = (delay_match.group(2) or delay_match.group(4) or "min").lower()
            if "hour" in unit:
                delay_sec = num * 3600
                timing_str = f"In {num} hour{'s' if num > 1 else ''}"
            elif "sec" in unit:
                delay_sec = num
                timing_str = f"In {num} seconds"
            else:
                delay_sec = num * 60
                timing_str = f"In {num} mins"

            # Sub-case 1: YouTube Search (e.g., 'open youtube and search pewdiepie in about 20 mins')
            if "youtube" in lower:
                yt_part = re.search(r"(?:search|find|play|watch)\s+['\"]?([A-Za-z0-9_\-\s]+?)['\"]?(?:\s+in\s+about|\s+in\s+\d+|\s+on\s+youtube|$)", text, re.IGNORECASE)
                query = yt_part.group(1).strip() if yt_part else "pewdiepie"
                return (
                    f"Scheduling YouTube search for '{query}' {timing_str.lower()}...",
                    ToolCallPayload(
                        tool="schedule_task",
                        arguments={
                            "name": f"YouTube: {query}",
                            "description": f"Open YouTube and search for '{query}'",
                            "action_tool": "browser_search",
                            "action_arguments": {"query": query, "site": "youtube"},
                            "delay_seconds": delay_sec,
                            "schedule_time": timing_str
                        }
                    )
                )

            # Sub-case 2: Gmail / Webmail (e.g., 'open googlechrome and open the gmail')
            if "gmail" in lower or "email" in lower:
                return (
                    f"Scheduling opening Gmail in Google Chrome {timing_str.lower()}...",
                    ToolCallPayload(
                        tool="schedule_task",
                        arguments={
                            "name": "Open Gmail",
                            "description": "Launch Google Chrome and open Gmail",
                            "action_tool": "open_browser",
                            "action_arguments": {"url": "https://mail.google.com"},
                            "delay_seconds": delay_sec,
                            "schedule_time": timing_str
                        }
                    )
                )

            # Sub-case 3: Open Website URL
            url_sched = re.search(r"(?:open\s+(?:website\s+|url\s+)?)(https?://[^\s]+|[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})", text, re.IGNORECASE)
            if url_sched:
                target_url = url_sched.group(1).strip()
                if not target_url.startswith("http"):
                    target_url = "https://" + target_url
                return (
                    f"Scheduling opening website '{target_url}' {timing_str.lower()}...",
                    ToolCallPayload(
                        tool="schedule_task",
                        arguments={
                            "name": f"Open {target_url}",
                            "description": f"Navigate browser to {target_url}",
                            "action_tool": "open_browser",
                            "action_arguments": {"url": target_url},
                            "delay_seconds": delay_sec,
                            "schedule_time": timing_str
                        }
                    )
                )

            # Sub-case 4: Open Application (e.g. 'open googlechrome', 'open chrome', 'launch spotify')
            app_sched = re.search(r"(?:open|launch|start)\s+(?:app(?:lication)?\s+)?([A-Za-z0-9\s]+?)(?:\s+by\s+the\s+time|\s+in\s+about|\s+in\s+\d+|$)", text, re.IGNORECASE)
            if app_sched:
                raw_app = app_sched.group(1).strip()
                if raw_app.lower() not in ["pc", "computer", "it", "window", "session"]:
                    return (
                        f"Scheduling launching '{raw_app}' {timing_str.lower()}...",
                        ToolCallPayload(
                            tool="schedule_task",
                            arguments={
                                "name": f"Launch {raw_app}",
                                "description": f"Launch application '{raw_app}'",
                                "action_tool": "open_application",
                                "action_arguments": {"app_name": raw_app},
                                "delay_seconds": delay_sec,
                                "schedule_time": timing_str
                            }
                        )
                    )

            # Sub-case 5: Lock PC
            if any(w in lower for w in ["lock pc", "lock computer", "lock screen"]):
                return (
                    f"Scheduling workstation lock {timing_str.lower()}...",
                    ToolCallPayload(
                        tool="schedule_task",
                        arguments={
                            "name": "Scheduled PC Lock",
                            "description": "Lock workstation session",
                            "action_tool": "lock_pc",
                            "action_arguments": {},
                            "delay_seconds": delay_sec,
                            "schedule_time": timing_str
                        }
                    )
                )

        # 1. System Telemetry / Status / RAM / CPU / Disk
        if any(w in lower for w in ["ram", "cpu", "disk", "specs", "status", "uptime", "telemetry", "hardware", "memory", "usage"]):
            return (
                "I'll query your computer's real-time hardware telemetry.",
                ToolCallPayload(tool="get_system_status", arguments={})
            )

        # 2. Screenshot
        if any(w in lower for w in ["screenshot", "screen capture", "capture screen", "snapshot"]):
            return (
                "Capturing current desktop display...",
                ToolCallPayload(tool="take_screenshot", arguments={})
            )

        # 3. Lock PC
        if any(w in lower for w in ["lock pc", "lock computer", "lock screen", "lock machine"]):
            return (
                "Locking your remote PC session.",
                ToolCallPayload(tool="lock_pc", arguments={})
            )

        # 4. Search YouTube in browser
        yt_match = re.search(r"(?:search|find|play).*?youtube.*?for\s+['\"]?([^'\"]+)['\"]?", text, re.IGNORECASE)
        if not yt_match:
            yt_match = re.search(r"youtube\s+(?:for\s+)?['\"]?([^'\"]+)['\"]?", text, re.IGNORECASE)
        if yt_match:
            q = yt_match.group(1).strip()
            return (
                f"Opening YouTube in your browser to search for '{q}'...",
                ToolCallPayload(tool="browser_search", arguments={"query": q, "site": "youtube"})
            )

        # 5. Search Google in browser
        search_match = re.search(r"(?:search\s+(?:google|web|the web)\s+(?:for\s+)?|google\s+)['\"]?([^'\"]+)['\"]?", text, re.IGNORECASE)
        if search_match:
            q = search_match.group(1).strip()
            return (
                f"Launching browser to search Google for '{q}'...",
                ToolCallPayload(tool="browser_search", arguments={"query": q, "site": "google"})
            )

        # 6. Open Browser with URL
        url_match = re.search(r"(?:open\s+(?:browser\s+to\s+|url\s+|website\s+)|navigate\s+to\s+)(https?://[^\s]+|[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})", text, re.IGNORECASE)
        if url_match:
            target_url = url_match.group(1).strip()
            if not target_url.startswith("http"):
                target_url = "https://" + target_url
            return (
                f"Opening browser navigating to {target_url}...",
                ToolCallPayload(tool="open_browser", arguments={"url": target_url})
            )

        # 7. Restart PC (High risk - requires confirmation)
        if "restart" in lower and any(w in lower for w in ["pc", "computer", "machine"]):
            return (
                "Restart PC requested. This will reboot your computer and requires confirmation.",
                ToolCallPayload(tool="restart_pc", arguments={})
            )

        # 8. Shutdown PC (High risk - requires confirmation)
        if "shutdown" in lower or "shut down" in lower:
            return (
                "Shutdown PC requested. This will power down your computer and requires confirmation.",
                ToolCallPayload(tool="shutdown_pc", arguments={})
            )

        # 9a. Minimize Application
        minimize_match = re.search(r"\b(?:minimize|hide)\s+(?:the\s+)?(?:app(?:lication)?\s+|window(?:s)?\s+(?:of\s+)?)?([A-Za-z0-9\s\-_]+)", text, re.IGNORECASE)
        if minimize_match and not any(k in lower for k in ["pc", "computer", "machine"]):
            raw_app = minimize_match.group(1).strip()
            raw_app = re.sub(r"\s+(?:or\s+close\s+(?:it|window)|app|application|window|please|now)$", "", raw_app, flags=re.IGNORECASE).strip()
            if raw_app:
                return (
                    f"Minimizing application '{raw_app}' on your computer...",
                    ToolCallPayload(tool="minimize_application", arguments={"app_name": raw_app})
                )

        # 9b. Close / Quit Application
        close_match = re.search(r"\b(?:close|quit|exit|terminate|kill|shut down)\s+(?:the\s+)?(?:app(?:lication)?\s+)?([A-Za-z0-9\s\-_]+)", text, re.IGNORECASE)
        if close_match and not any(k in lower for k in ["pc", "computer", "machine", "session"]):
            raw_app = close_match.group(1).strip()
            raw_app = re.sub(r"\s+(?:or\s+minimize\s+(?:it|window)|app|application|window|please|now)$", "", raw_app, flags=re.IGNORECASE).strip()
            if raw_app and raw_app.lower() not in ["pc", "computer"]:
                return (
                    f"Closing application '{raw_app}' on your computer...",
                    ToolCallPayload(tool="close_application", arguments={"app_name": raw_app})
                )

        # 10. Open Application
        app_match = re.search(r"\b(?:open|launch|start)\s+(?:app(?:lication)?\s+)?([A-Za-z0-9\s]+)", text, re.IGNORECASE)
        if app_match and not any(k in lower for k in ["youtube", "google", "file", "document", "folder", "restart", "shutdown", "browser", "resume"]):
            app_name = app_match.group(1).strip()
            return (
                f"Launching application '{app_name}' on your computer...",
                ToolCallPayload(tool="open_application", arguments={"app_name": app_name})
            )

        # 11. Fun & Voice: Say text / Text-to-Speech
        say_match = re.search(r"\b(?:say|speak|tell pc to say|tell mac to say|pronounce)\s+['\"]?(.+?)['\"]?$", text, re.IGNORECASE)
        if say_match:
            speech_text = say_match.group(1).strip()
            return (
                f"Speaking '{speech_text}' on your computer speakers...",
                ToolCallPayload(tool="say_text", arguments={"text": speech_text})
            )

        # 12. Fun & Alert: Play chime / ping sound
        if any(w in lower for w in ["play sound", "play chime", "play alert", "ping mac", "ping pc", "chime", "ring bell", "alert sound"]):
            return (
                "Playing alert chime on your workstation...",
                ToolCallPayload(tool="play_alert", arguments={"sound": "Ping"})
            )

        # 13. Desktop Alert / Banner Notification
        notify_match = re.search(r"\b(?:show\s+(?:desktop\s+)?(?:notification|alert)|display\s+(?:desktop\s+)?(?:notification|alert)|desktop\s+alert|send\s+(?:desktop\s+)?(?:notification|alert)|notify\s+me|alert\s+me)(?:\s+(?:that|with|saying|message))?\s+['\"]?(.+?)['\"]?$", text, re.IGNORECASE)
        if notify_match:
            notif_text = notify_match.group(1).strip()
            return (
                f"Displaying desktop alert '{notif_text}' on your computer...",
                ToolCallPayload(tool="show_notification", arguments={"title": "RemoteForge", "message": notif_text})
            )
        if any(w in lower for w in ["desktop alert", "show alert", "display alert", "send alert", "test alert"]):
            return (
                "Displaying desktop alert on your computer...",
                ToolCallPayload(tool="show_notification", arguments={"title": "RemoteForge", "message": "Alert from RemoteForge!"})
            )

        # 14. Volume Control
        vol_match = re.search(r"\b(?:set\s+)?volume\s+(?:to\s+)?(\d+)(?:%)?", text, re.IGNORECASE)
        if vol_match:
            lvl = int(vol_match.group(1))
            return (
                f"Setting workstation audio volume to {lvl}%...",
                ToolCallPayload(tool="set_volume", arguments={"level": lvl})
            )
        if "mute" in lower:
            return (
                "Muting workstation audio...",
                ToolCallPayload(tool="set_volume", arguments={"level": 0})
            )

        # 15. Shell Command Execution
        cmd_match = re.search(r"\b(?:run|execute)\s+(?:command|cmd|shell|terminal)\s+['\"]?(.+?)['\"]?$", text, re.IGNORECASE)
        if cmd_match:
            c = cmd_match.group(1).strip()
            return (
                f"Executing command `{c}` on your computer...",
                ToolCallPayload(tool="run_command", arguments={"command": c})
            )

        # 16. Disambiguation response check (e.g., user replying "pdf", "word", "docx", "1", "2")
        disambig_choice = lower.replace("the ", "").replace("open ", "").replace("show ", "").strip()
        if disambig_choice in ["pdf", "word", "docx", "ppt", "pptx", "1", "2", "first one", "second one"] or any(k in disambig_choice for k in ["pdf version", "word version", "the pdf", "the word"]):
            base_q = getattr(self, "last_search_query", "resume")
            chosen_ext = "pdf" if ("pdf" in disambig_choice or disambig_choice in ["1", "first one"]) else "docx"
            return (
                f"Opening the {chosen_ext.upper()} version of '{base_q}'...",
                ToolCallPayload(tool="search_files", arguments={"query": f"{base_q}.{chosen_ext}"})
            )

        # 17. Natural Language File Search (resume, assignment, invoices, documents, etc.)
        common_file_nouns = ["resume", "assignment", "report", "presentation", "syllabus", "notes", "thesis", "invoice", "receipt", "document", "paper"]
        is_direct_file_keyword = any(k in lower for k in common_file_nouns)
        file_query_match = re.search(r"\b(?:find|search|locate|where is|check if|do i have|look for|get|show|open|preview)\s+(?:my\s+)?(?:the\s+)?([A-Za-z0-9_\-\s]+?)(?:\s+file|\s+document|\s+on my pc|\s+on computer|\s+in documents|\s+in downloads)?$", text, re.IGNORECASE)

        if is_direct_file_keyword or file_query_match:
            raw_target = file_query_match.group(1).strip() if file_query_match else text
            # Clean filler words
            clean_q = re.sub(r"^(find|search for|search|locate|show|where is|do i have|get|open|my|the)\s+", "", raw_target, flags=re.IGNORECASE).strip()
            clean_q = re.sub(r"\s+(?:in|on)\s+(?:my\s+)?(?:files|pc|computer|documents)$", "", clean_q, flags=re.IGNORECASE).strip()
            clean_q = re.sub(r"\s+(?:file|files|document|documents)$", "", clean_q, flags=re.IGNORECASE).strip()
            # Strip file extension if specified so search finds all variations
            clean_q = re.sub(r"\.(pdf|docx?|pptx?|xlsx?|txt)$", "", clean_q, flags=re.IGNORECASE).strip()

            if not clean_q and is_direct_file_keyword:
                for noun in common_file_nouns:
                    if noun in lower:
                        clean_q = noun
                        break

            if clean_q:
                self.last_search_query = clean_q
                return (
                    f"Searching your user folders for '{clean_q}'...",
                    ToolCallPayload(tool="search_files", arguments={"query": clean_q})
                )

        # 18. List Directory / Folders
        if any(w in lower for w in ["list files", "show files", "browse files", "list directory", "what files"]):
            return (
                "Retrieving files from your allowed user directories...",
                ToolCallPayload(tool="list_directory", arguments={})
            )

        # Default conversational reply
        return (
            "I'm RemoteForge AI. You can say 'resume' or 'find assignment' to search files, 'close Chrome', 'say hello on mac', 'play alert chime', 'capture screenshot', or check real-time CPU/RAM telemetry. What would you like to do?",
            None
        )

class OpenAICompatibleProvider(BaseAIProvider):
    def __init__(self, api_key: str, base_url: str = "https://api.openai.com/v1", model: str = "gpt-4o-mini"):
        self.api_key = api_key
        self.base_url = base_url.rstrip("/")
        self.model = model

    async def process_message(
        self,
        message: str,
        history: List[Dict[str, str]]
    ) -> Tuple[str, Optional[ToolCallPayload]]:
        import urllib.request
        import urllib.error

        messages = [{"role": "system", "content": SYSTEM_PROMPT}]
        for h in history[-8:]:
            messages.append({"role": h.get("role", "user"), "content": h.get("content", "")})
        messages.append({"role": "user", "content": message})

        payload = {
            "model": self.model,
            "messages": messages,
            "tools": AI_AVAILABLE_TOOLS,
            "tool_choice": "auto"
        }

        req = urllib.request.Request(
            f"{self.base_url}/chat/completions",
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {self.api_key}"
            }
        )

        try:
            with urllib.request.urlopen(req, timeout=15) as resp:
                data = json.loads(resp.read().decode("utf-8"))
                choice = data["choices"][0]["message"]
                content = choice.get("content") or ""

                tool_calls = choice.get("tool_calls")
                if tool_calls and len(tool_calls) > 0:
                    fn = tool_calls[0]["function"]
                    t_name = fn["name"]
                    t_args = json.loads(fn.get("arguments", "{}"))
                    return content or f"Calling tool {t_name}...", ToolCallPayload(tool=t_name, arguments=t_args)

                return content, None
        except Exception as e:
            logger.warning(f"OpenAICompatibleProvider error: {e}. Falling back to LocalRuleAIProvider.")
            fallback = LocalRuleAIProvider()
            return await fallback.process_message(message, history)

def get_ai_provider() -> BaseAIProvider:
    # 1. AI Skill Engine Cloud Orchestration Layer
    skill_engine_key = os.getenv("AI_SKILL_ENGINE_API_KEY", "").strip()
    ai_provider_mode = os.getenv("AI_PROVIDER", "").strip().lower()
    if skill_engine_key or ai_provider_mode == "skill_engine":
        from app.ai.skill_engine_gateway import AISkillEngineGateway
        return AISkillEngineGateway(
            base_url=os.getenv("AI_SKILL_ENGINE_URL", "http://localhost:2704/api/v1"),
            api_key=skill_engine_key,
            model=os.getenv("AI_SKILL_ENGINE_MODEL", "gemini-2.5-flash"),
            app_id=os.getenv("AI_SKILL_ENGINE_APP_ID", None)
        )

    # 2. Generic OpenAI-compatible Provider
    api_key = os.getenv("AI_API_KEY", "").strip()
    if api_key:
        base_url = os.getenv("AI_BASE_URL", "https://api.openai.com/v1")
        model = os.getenv("AI_MODEL", "gpt-4o-mini")
        return OpenAICompatibleProvider(api_key=api_key, base_url=base_url, model=model)

    # 3. Fast built-in offline zero-dependency Rule Provider
    return LocalRuleAIProvider()
