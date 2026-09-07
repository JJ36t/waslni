"""V1 API router — aggregates all v1 sub-routers.

Phase 8 adds /auth. Phase 9 will add /customers and /deliveries.
Phase 10 will add /sync.
"""
from fastapi import APIRouter

from app.api.auth import router as auth_router

router = APIRouter()
router.include_router(auth_router)


@router.get("/health", tags=["meta"])
async def api_health() -> dict[str, str]:
    """Lightweight liveness check inside /api/v1."""
    return {"status": "ok"}
