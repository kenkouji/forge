from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.database.session import get_db
from app.models.entities import User
from app.auth.dependencies import get_current_user
from app.ai.schemas import ChatRequest, ChatResponse
from app.ai.engine import ai_engine
from app.ai.tools import AI_AVAILABLE_TOOLS

router = APIRouter(prefix="/ai", tags=["AI Assistant"])

@router.post("/chat", response_model=ChatResponse)
async def chat_with_assistant(
    request: ChatRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """
    Interacts with RemoteForge AI Assistant, processing natural language,
    invoking structured tools on the target PC, and returning real results.
    """
    try:
        response = await ai_engine.handle_user_message(
            request=request,
            db=db,
            actor=current_user.username
        )
        return response
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/tools")
def get_supported_tools(
    current_user: User = Depends(get_current_user)
):
    return AI_AVAILABLE_TOOLS
