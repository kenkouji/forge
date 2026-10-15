from fastapi import APIRouter

router = APIRouter(tags=["Health"])

@router.get("/health")
def get_health():
    return {
        "status": "ok",
        "server": "RemoteForge",
        "version": "0.1.0"
    }
