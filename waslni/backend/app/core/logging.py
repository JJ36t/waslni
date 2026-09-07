"""Structured logging configuration.

Uses structlog for JSON-formatted logs in production, pretty-printed in dev.
Logs are NEVER written with sensitive fields — that's the responsibility of
the calling code, not this module.
"""
import logging
import sys

from app.core.config import settings


def configure_logging() -> None:
    """Configure structlog + stdlib logging.

    Call once at app startup (lifespan handler).
    """
    level = getattr(logging, settings.LOG_LEVEL.upper(), logging.INFO)

    if settings.LOG_FORMAT == "json":
        _configure_json_logging(level)
    else:
        _configure_text_logging(level)


def _configure_json_logging(level: int) -> None:
    """JSON logs — production / structured log collector friendly."""
    try:
        import structlog

        structlog.configure(
            processors=[
                structlog.contextvars.merge_contextvars,
                structlog.processors.add_log_level,
                structlog.processors.TimeStamper(fmt="iso"),
                structlog.processors.StackInfoRenderer(),
                structlog.processors.format_exc_info,
                structlog.processors.JSONRenderer(),
            ],
            wrapper_class=structlog.make_filtering_bound_logger(level),
            cache_logger_on_first_use=True,
        )

        # Bridge stdlib logging → structlog
        logging.basicConfig(
            level=level,
            stream=sys.stdout,
            format="%(message)s",
        )
        for noisy in ("uvicorn.access",):
            logging.getLogger(noisy).setLevel(logging.WARNING)
    except ImportError:
        _configure_text_logging(level)


def _configure_text_logging(level: int) -> None:
    """Human-readable text logs — development."""
    logging.basicConfig(
        level=level,
        stream=sys.stdout,
        format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )
