from typing import Dict, Any, List, Optional
from pydantic import BaseModel, Field

class ToolCallPayload(BaseModel):
    tool: str
    arguments: Dict[str, Any] = Field(default_factory=dict)

class ToolResultPayload(BaseModel):
    tool: str
    success: bool
    result: Optional[Any] = None
    error: Optional[str] = None

class ChatRequest(BaseModel):
    message: str
    device_id: Optional[str] = None
    confirmed: bool = False
    pending_tool: Optional[str] = None
    pending_arguments: Optional[Dict[str, Any]] = None

class ChatResponse(BaseModel):
    reply: str
    tool_called: Optional[str] = None
    tool_arguments: Optional[Dict[str, Any]] = None
    tool_result: Optional[Any] = None
    requires_confirmation: bool = False
    confirmation_prompt: Optional[str] = None
    error: Optional[str] = None
