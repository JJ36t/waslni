"""FastAPI application factory and entry point.

Wires together:
  - CORS middleware
  - Exception handlers (unified error model)
  - Routers (mounted under /api/v1)
  - /health endpoint (root level)
  - Structured logging
"""
from contextlib import asynccontextmanager
from collections.abc import AsyncGenerator

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from sqlalchemy.exc import SQLAlchemyError

from app.api.v1 import router as v1_router
from app.core.config import settings
from app.core.database import check_db_connection
from app.core.exceptions import (
    AppError,
    ErrorCodes,
    format_error_response,
)
from app.core.logging import configure_logging


# === Lifespan ===
@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    """Application startup + shutdown hooks."""
    configure_logging()
    import logging

    logging.getLogger(__name__).info(
        "Starting Waselni backend",
        extra={"env": settings.APP_ENV, "debug": settings.APP_DEBUG},
    )
    yield
    logging.getLogger(__name__).info("Shutting down Waselni backend")


# === App ===
def create_app() -> FastAPI:
    """Application factory — used by uvicorn / gunicorn / tests."""
    app = FastAPI(
        title="Waselni Backend",
        description="Offline-first delivery navigation API for the Waselni driver app.",
        version="1.0.0",
        docs_url="/docs" if not settings.is_production else None,
        redoc_url="/redoc" if not settings.is_production else None,
        openapi_url="/openapi.json" if not settings.is_production else None,
        lifespan=lifespan,
    )

    # === Middleware ===
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_origins_list,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
        expose_headers=["X-Request-Id", "X-RateLimit-Remaining"],
    )

    # === Routers ===
    app.include_router(v1_router, prefix="/api/v1", tags=["v1"])

    # === Exception handlers ===
    @app.exception_handler(AppError)
    async def app_error_handler(request: Request, exc: AppError) -> JSONResponse:
        """Convert AppError subclasses to the unified error response shape."""
        return JSONResponse(
            status_code=exc.status_code,
            content=format_error_response(exc.code, exc.message, exc.details),
        )

    @app.exception_handler(SQLAlchemyError)
    async def db_error_handler(request: Request, exc: SQLAlchemyError) -> JSONResponse:
        """Don't leak DB internals — return a generic 500."""
        import logging

        logging.getLogger(__name__).exception("Database error", exc_info=exc)
        return JSONResponse(
            status_code=500,
            content=format_error_response(
                ErrorCodes.INTERNAL_ERROR,
                "Internal server error",
            ),
        )

    @app.exception_handler(Exception)
    async def unhandled_error_handler(request: Request, exc: Exception) -> JSONResponse:
        """Catch-all for unhandled exceptions — never leak internals."""
        import logging

        logging.getLogger(__name__).exception("Unhandled exception", exc_info=exc)
        return JSONResponse(
            status_code=500,
            content=format_error_response(
                ErrorCodes.INTERNAL_ERROR,
                "Internal server error",
            ),
        )

    # === Root-level health check (NOT under /api/v1) ===
    @app.get("/health", tags=["meta"])
    async def root_health() -> dict[str, object]:
        """Liveness + DB readiness probe.

        Used by Docker healthcheck and the load balancer.
        """
        db_ok = await check_db_connection()
        return {
            "status": "ok" if db_ok else "degraded",
            "db": "ok" if db_ok else "unreachable",
            "env": settings.APP_ENV,
            "version": "1.0.0",
        }

    return app


# Module-level instance — what uvicorn loads.
app = create_app()
