"""V1 API router — aggregates all v1 sub-routers.

Phase 8:  /auth
Phase 9:  /customers + /deliveries
Phase 10: /sync
"""
from fastapi import APIRouter

from app.api.auth import router as auth_router
from app.api.customers import router as customers_router
from app.api.deliveries import router as deliveries_router
from app.api.sync import router as sync_router

router = APIRouter()
router.include_router(auth_router)
router.include_router(customers_router)
router.include_router(deliveries_router)
router.include_router(sync_router)


@router.get("/health", tags=["meta"])
async def api_health() -> dict[str, str]:
    """Lightweight liveness check inside /api/v1."""
    return {"status": "ok"}
