from enum import Enum
from typing import Dict, Any, Optional

class PermissionLevel(str, Enum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"

# Registered Controlled Tools with risk classification and confirmation requirements
TOOL_DEFINITIONS: Dict[str, Dict[str, Any]] = {
    # System Status & Info (LOW)
    "get_system_status": {
        "permission": PermissionLevel.LOW,
        "requires_confirmation": False,
        "description": "Retrieve CPU, RAM, Disk, OS, and latency metrics without sensitive data.",
        "allowed_arguments": []
    },
    "take_screenshot": {
        "permission": PermissionLevel.LOW,
        "requires_confirmation": False,
        "description": "Capture current PC display screenshot.",
        "allowed_arguments": []
    },
    "lock_pc": {
        "permission": PermissionLevel.LOW,
        "requires_confirmation": False,
        "description": "Lock the user session on PC.",
        "allowed_arguments": []
    },
    "schedule_task": {
        "permission": PermissionLevel.LOW,
        "requires_confirmation": False,
        "description": "Schedule a delayed or timed automation task.",
        "allowed_arguments": ["name", "description", "action_tool", "action_arguments", "delay_seconds", "schedule_time"]
    },

    # Application Control (LOW / MEDIUM)
    "open_application": {
        "permission": PermissionLevel.LOW,
        "requires_confirmation": False,
        "description": "Launch an allowed installed application by name.",
        "allowed_arguments": ["app_name"]
    },
    "close_application": {
        "permission": PermissionLevel.MEDIUM,
        "requires_confirmation": False,
        "description": "Gracefully request an application to terminate.",
        "allowed_arguments": ["app_name"]
    },
    "minimize_application": {
        "permission": PermissionLevel.LOW,
        "requires_confirmation": False,
        "description": "Minimize application windows or hide application.",
        "allowed_arguments": ["app_name"]
    },
    "run_command": {
        "permission": PermissionLevel.MEDIUM,
        "requires_confirmation": False,
        "description": "Execute a shell command on the workstation and capture output.",
        "allowed_arguments": ["command", "cmd"]
    },
    # Fun & Multimedia Remote Tools (LOW)
    "say_text": {
        "permission": PermissionLevel.LOW,
        "requires_confirmation": False,
        "description": "Speak text aloud on the workstation speakers via text-to-speech.",
        "allowed_arguments": ["text"]
    },
    "play_alert": {
        "permission": PermissionLevel.LOW,
        "requires_confirmation": False,
        "description": "Play an alert chime or ping sound on the workstation.",
        "allowed_arguments": ["sound"]
    },
    "show_notification": {
        "permission": PermissionLevel.LOW,
        "requires_confirmation": False,
        "description": "Display a native desktop banner notification.",
        "allowed_arguments": ["title", "message"]
    },
    "set_volume": {
        "permission": PermissionLevel.LOW,
        "requires_confirmation": False,
        "description": "Adjust system audio volume level.",
        "allowed_arguments": ["level"]
    },
    "send_input_event": {
        "permission": PermissionLevel.LOW,
        "requires_confirmation": False,
        "description": "Send authenticated remote touchpad or keyboard input event.",
        "allowed_arguments": ["action", "dx", "dy", "x", "y", "button", "click_count", "is_drag", "key", "modifiers", "name", "text", "timestamp"]
    },

    # Controlled Browser Automation (LOW)
    "open_browser": {
        "permission": PermissionLevel.LOW,
        "requires_confirmation": False,
        "description": "Launch browser to a specific URL.",
        "allowed_arguments": ["url"]
    },
    "browser_search": {
        "permission": PermissionLevel.LOW,
        "requires_confirmation": False,
        "description": "Perform a search query in browser.",
        "allowed_arguments": ["query", "site"]
    },
    "browser_navigate": {
        "permission": PermissionLevel.LOW,
        "requires_confirmation": False,
        "description": "Navigate active browser session to URL.",
        "allowed_arguments": ["url"]
    },

    # User-Data Only Filesystem (LOW / MEDIUM)
    "search_files": {
        "permission": PermissionLevel.LOW,
        "requires_confirmation": False,
        "description": "Search filenames within strictly allowed user directories.",
        "allowed_arguments": ["query", "directory"]
    },
    "get_file_metadata": {
        "permission": PermissionLevel.LOW,
        "requires_confirmation": False,
        "description": "Retrieve metadata (size, mod_time, type) of an allowed user file.",
        "allowed_arguments": ["path"]
    },
    "list_directory": {
        "permission": PermissionLevel.LOW,
        "requires_confirmation": False,
        "description": "List files and subfolders within an allowed root.",
        "allowed_arguments": ["path"]
    },
    "download_file": {
        "permission": PermissionLevel.MEDIUM,
        "requires_confirmation": False,
        "description": "Stream an authorized user file from PC to requesting client.",
        "allowed_arguments": ["path"]
    },
    "download_file_chunk": {
        "permission": PermissionLevel.MEDIUM,
        "requires_confirmation": False,
        "description": "Read a bounded chunk of an allowed user file.",
        "allowed_arguments": ["path", "offset", "length"]
    },
    "upload_file": {
        "permission": PermissionLevel.MEDIUM,
        "requires_confirmation": False,
        "description": "Upload a file into an authorized user folder.",
        "allowed_arguments": ["target_directory", "filename"]
    },
    "upload_file_chunk": {
        "permission": PermissionLevel.MEDIUM,
        "requires_confirmation": False,
        "description": "Stream an upload chunk to an authorized user folder.",
        "allowed_arguments": ["target_directory", "filename", "transfer_id", "chunk_data_base64", "offset", "is_first", "is_last"]
    },
    "cancel_file_upload": {
        "permission": PermissionLevel.MEDIUM,
        "requires_confirmation": False,
        "description": "Cancel an in-progress file upload and delete temp files.",
        "allowed_arguments": ["transfer_id"]
    },
    "get_file_hash": {
        "permission": PermissionLevel.LOW,
        "requires_confirmation": False,
        "description": "Calculate SHA-256 verification hash of an allowed user file.",
        "allowed_arguments": ["path"]
    },
    "zip_directory": {
        "permission": PermissionLevel.MEDIUM,
        "requires_confirmation": False,
        "description": "Create a zip archive of an allowed user directory for download.",
        "allowed_arguments": ["path", "target_directory"]
    },
    "get_allowed_directories": {
        "permission": PermissionLevel.LOW,
        "requires_confirmation": False,
        "description": "List all configured allowed user data directories on PC.",
        "allowed_arguments": []
    },
    "add_allowed_directory": {
        "permission": PermissionLevel.MEDIUM,
        "requires_confirmation": True,
        "description": "Add an explicitly chosen user directory to allowlist.",
        "allowed_arguments": ["path", "display_name"]
    },

    # High-Risk Power & System Operations (HIGH - Explicit Confirmation Required)
    "restart_pc": {
        "permission": PermissionLevel.HIGH,
        "requires_confirmation": True,
        "description": "Reboot the remote computer.",
        "allowed_arguments": []
    },
    "shutdown_pc": {
        "permission": PermissionLevel.HIGH,
        "requires_confirmation": True,
        "description": "Gracefully shut down the remote computer.",
        "allowed_arguments": []
    }
}

FORBIDDEN_COMMAND_PATTERNS = [
    "sh", "bash", "zsh", "powershell", "cmd", "cmd.exe",
    "sudo", "rm", "chmod", "chown", "curl", "wget", "kill",
    "exec", "eval", "system", "spawn"
]

def is_tool_registered(tool_name: str) -> bool:
    return tool_name in TOOL_DEFINITIONS

def get_tool_definition(tool_name: str) -> Optional[Dict[str, Any]]:
    return TOOL_DEFINITIONS.get(tool_name)

def validate_tool_and_permissions(tool_name: str, arguments: Dict[str, Any], is_confirmed: bool = False) -> Dict[str, Any]:
    if not is_tool_registered(tool_name):
        return {
            "valid": False,
            "error": f"Tool '{tool_name}' is not in the controlled tool registry. Arbitrary commands are strictly forbidden.",
            "permission_level": "UNKNOWN",
            "requires_confirmation": False
        }

    tool_def = TOOL_DEFINITIONS[tool_name]
    req_confirmation = tool_def["requires_confirmation"]

    if req_confirmation and not is_confirmed:
        return {
            "valid": False,
            "error": f"Action '{tool_name}' is classified as HIGH/CONFIRMATION risk and requires explicit user confirmation.",
            "permission_level": tool_def["permission"].value,
            "requires_confirmation": True
        }

    # Catastrophic system wipe patterns
    CATASTROPHIC_PATTERNS = ["rm -rf /", "rm -rf /*", "mkfs", ":(){ :|:& };:", "> /dev/sda", "> /dev/null", "chmod -R 777 /"]
    for arg_name, arg_val in arguments.items():
        if isinstance(arg_val, str):
            for cat in CATASTROPHIC_PATTERNS:
                if cat in arg_val:
                    return {
                        "valid": False,
                        "error": f"Disallowed shell pattern: Catastrophic command pattern '{cat}' is forbidden.",
                        "permission_level": tool_def["permission"].value,
                        "requires_confirmation": req_confirmation
                    }

    if tool_name != "run_command":
        # Verify no arbitrary sub-command injections in string arguments for specialized tools
        for arg_name, arg_val in arguments.items():
            if isinstance(arg_val, str):
                for forbidden in FORBIDDEN_COMMAND_PATTERNS:
                    if forbidden in arg_val.split():
                        return {
                            "valid": False,
                            "error": f"Disallowed shell pattern '{forbidden}' detected in argument '{arg_name}'.",
                            "permission_level": tool_def["permission"].value,
                            "requires_confirmation": req_confirmation
                        }

    return {
        "valid": True,
        "error": None,
        "permission_level": tool_def["permission"].value,
        "requires_confirmation": req_confirmation
    }
