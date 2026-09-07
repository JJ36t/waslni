"""Empty router placeholder for the v1 API.

Phase 8 will add the /auth router here. Phase 9 will add /customers and
/deliveries. Phase 10 will add /sync.
"""
from fastapi import APIRouter

router = APIRouter()


@router.get("/health", tags=["meta"])
async def api_health() -> dict[str, str]:
    """Lightweight liveness check inside /api/v1."""
    return {"status": "ok"}
