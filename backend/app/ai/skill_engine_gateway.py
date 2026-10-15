"""
RemoteForge AI Skill Engine Cloud Gateway
=========================================
Integrates sandeshnaroju/ai-skill-engine (OpenAI-compatible AI gateway & skill engine)
with RemoteForge's zero-trust security architecture.

Key Responsibilities & Security Guarantees:
1. Orchestration: Delegates natural language planning to AI Skill Engine over its
   authenticated OpenAI-compatible `/api/v1/chat/completions` endpoint.
2. Zero-Trust Tool Interception: Model-proposed tool calls are never executed directly
   by AI Skill Engine. RemoteForge intercepts every planned tool call before execution.
3. Policy Enforcement Point (PEP):
   - Rejects unregistered, arbitrary shell or code execution tools.
   - Validates arguments against strict schemas.
   - Bounds directory operations to user allowlisted folders.
   - Enforces human confirmation for consequential actions.
4. Tenant Security:
   - AI Skill Engine API keys (`sk_mgr_...`) are held exclusively by the RemoteForge backend
     and NEVER exposed to the mobile APK.
"""

import os
import json
import logging
from typing import Dict, Any, List, Optional, Tuple
import urllib.request
import urllib.error

from app.ai.provider import BaseAIProvider, LocalRuleAIProvider
from app.ai.schemas import ToolCallPayload
from app.ai.prompts import SYSTEM_PROMPT
from app.ai.tools import AI_AVAILABLE_TOOLS
from app.permissions.registry import is_tool_registered, validate_tool_and_permissions

logger = logging.getLogger("remoteforge.ai.skill_engine_gateway")


class AISkillEngineSecurityError(Exception):
    """Raised when an AI Skill Engine response violates RemoteForge security policies."""
    pass


class AISkillEngineGateway(BaseAIProvider):
    """
    Connects RemoteForge to a self-hosted or cloud-hosted AI Skill Engine instance.
    Uses the standard OpenAI wire format exposed by AI Skill Engine at /api/v1/chat/completions.
    """

    def __init__(
        self,
        base_url: Optional[str] = None,
        api_key: Optional[str] = None,
        model: Optional[str] = None,
        app_id: Optional[str] = None,
        allowed_tools: Optional[List[Dict[str, Any]]] = None,
        simulate_offline: bool = False
    ):
        self.base_url = (base_url or os.getenv("AI_SKILL_ENGINE_URL", "http://localhost:2704/api/v1")).rstrip("/")
        self.api_key = api_key or os.getenv("AI_SKILL_ENGINE_API_KEY", "")
        self.model = model or os.getenv("AI_SKILL_ENGINE_MODEL", "gemini-2.5-flash")
        self.app_id = app_id or os.getenv("AI_SKILL_ENGINE_APP_ID", None)
        self.simulate_offline = simulate_offline

        # Restrict tools exclusively to approved, safe RemoteForge workstation tools
        # Unrestricted code interpreters and shell sandboxes are strictly excluded
        raw_tools = allowed_tools or AI_AVAILABLE_TOOLS
        self.permitted_tools = [
            t for t in raw_tools
            if t.get("function", {}).get("name") not in ("code_interpreter", "run_shell", "execute_code")
        ]

    def validate_and_intercept_tool_call(
        self,
        tool_name: str,
        arguments: Dict[str, Any]
    ) -> Tuple[bool, Optional[str], Dict[str, Any]]:
        """
        Independent Policy Enforcement Point (PEP) outside the AI model.
        Returns: (is_permitted, error_message, sanitized_arguments)
        """
        # 1. Reject unregistered tools
        if not is_tool_registered(tool_name):
            logger.warning(f"[Security Gate]: Model proposed unregistered tool '{tool_name}' - REJECTED")
            return False, f"Policy Violation: Tool '{tool_name}' is not permitted on this workstation.", {}

        # 2. Block arbitrary shell or code execution
        if tool_name in ("run_shell", "code_interpreter", "execute_python", "execute_code"):
            logger.critical(f"[Security Gate]: Blocked arbitrary execution tool '{tool_name}'")
            return False, "Arbitrary shell or code execution is disabled by security policy.", {}

        # 3. Path Traversal & Bounded Directory Verification
        if "path" in arguments or "start_directory" in arguments or "directory" in arguments:
            path_val = str(arguments.get("path") or arguments.get("start_directory") or arguments.get("directory") or "")
            forbidden_prefixes = ["/etc", "/private", "/System", "/Library", "/var", "C:\\Windows", ".ssh", ".aws", ".env"]
            if any(forbidden in path_val for forbidden in forbidden_prefixes):
                logger.warning(f"[Security Gate]: Path traversal attempt blocked: '{path_val}'")
                return False, f"Access to system or sensitive directory '{path_val}' is prohibited.", {}

        # 4. Enforce Permission Registry schema validation
        policy_res = validate_tool_and_permissions(tool_name, arguments, is_confirmed=False)
        if not policy_res.get("valid"):
            return False, policy_res.get("error", "Tool argument validation failed."), {}

        return True, None, arguments

    async def process_message(
        self,
        message: str,
        history: List[Dict[str, str]]
    ) -> Tuple[str, Optional[ToolCallPayload]]:
        """
        Sends message to AI Skill Engine /api/v1/chat/completions, parses tool proposal,
        and enforces zero-trust security checks before returning.
        """
        # Offline or simulator mode for testing
        if self.simulate_offline or not self.api_key:
            return await self._simulate_or_fallback(message, history)

        messages = [{"role": "system", "content": SYSTEM_PROMPT}]
        for turn in history[-6:]:
            messages.append({"role": turn.get("role", "user"), "content": turn.get("content", "")})
        messages.append({"role": "user", "content": message})

        payload = {
            "model": self.model,
            "messages": messages,
            "tools": self.permitted_tools,
            "tool_choice": "auto"
        }
        if self.app_id:
            payload["app_id"] = self.app_id

        endpoint = f"{self.base_url}/chat/completions"
        req = urllib.request.Request(
            endpoint,
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {self.api_key}",
                "X-Request-Source": "remoteforge-gateway"
            }
        )

        try:
            with urllib.request.urlopen(req, timeout=15) as resp:
                data = json.loads(resp.read().decode("utf-8"))
                choice = data.get("choices", [{}])[0].get("message", {})
                content = choice.get("content") or ""

                tool_calls = choice.get("tool_calls")
                if tool_calls and len(tool_calls) > 0:
                    fn = tool_calls[0].get("function", {})
                    t_name = fn.get("name")
                    try:
                        t_args = json.loads(fn.get("arguments", "{}"))
                    except Exception:
                        t_args = {}

                    # Intercept and validate tool call outside the model
                    permitted, err_msg, sanitized_args = self.validate_and_intercept_tool_call(t_name, t_args)
                    if not permitted:
                        return f"Security Policy Block: {err_msg}", None

                    return content or f"Executing {t_name}...", ToolCallPayload(tool=t_name, arguments=sanitized_args)

                return content, None

        except urllib.error.URLError as e:
            logger.warning(f"AI Skill Engine connection failed ({e}). Falling back to LocalRuleAIProvider.")
            return await self._simulate_or_fallback(message, history)
        except Exception as e:
            logger.error(f"Unexpected AI Skill Engine error: {e}. Falling back.")
            return await self._simulate_or_fallback(message, history)

    async def _simulate_or_fallback(
        self,
        message: str,
        history: List[Dict[str, str]]
    ) -> Tuple[str, Optional[ToolCallPayload]]:
        """Fallback to local intelligent rule-based engine when AI Skill Engine service is unreachable."""
        fallback = LocalRuleAIProvider()
        return await fallback.process_message(message, history)
