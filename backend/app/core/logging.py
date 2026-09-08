"""Structured logging configuration.

Uses structlog for JSON-formatted logs in production, pretty-printed in dev.
Logs are NEVER written with sensitive fields — that's the responsibility of
the calling code, not this module.

Log levels:
  - Development: DEBUG (verbose — includes SQL queries via SQLAlchemy echo)
  - Staging: INFO
  - Production: WARNING (only warnings + errors → smaller log volume)

JSON format (production):
  {"event": "login_success", "level": "info", "timestamp": "2026-09-08T10:30:00Z",
   "user_id": "abc-123", "ip": "1.2.3.4"}

Sensitive fields that must NEVER appear in logs:
  - password / password_hash
  - access_token / refresh_token
  - phone numbers
  - GPS coordinates
  - JWT_SECRET
"""
import logging
import sys

from app.core.config import settings


# Fields that should never appear in log output.
SENSITIVE_FIELDS = frozenset({
    "password", "password_hash", "access_token", "refresh_token",
    "jwt_secret", "token", "authorization",
    "phone", "latitude", "longitude", "accuracy",
})


def configure_logging() -> None:
    """Configure structlog + stdlib logging.

    Call once at app startup (lifespan handler).
    """
    level = getattr(logging, settings.LOG_LEVEL.upper(), logging.INFO)

    if settings.LOG_FORMAT == "json":
        _configure_json_logging(level)
    else:
        _configure_text_logging(level)


def get_logger(name: str) -> "structlog.stdlib.BoundLogger":
    """Get a structured logger.

    Usage:
        from app.core.logging import get_logger
        logger = get_logger(__name__)
        logger.info("login_success", user_id=user.id, ip=ip_address)
    """
    try:
        import structlog
        return structlog.get_logger(name)
    except ImportError:
        return logging.getLogger(name)


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
                _redact_sensitive_fields,
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

        # Reduce noise from framework loggers
        for noisy in ("uvicorn.access", "sqlalchemy.engine"):
            logging.getLogger(noisy).setLevel(logging.WARNING)

    except ImportError:
        _configure_text_logging(level)


def _redact_sensitive_fields(logger, method_name, event_dict):
    """Structlog processor that redacts sensitive field values.

    Replaces any key in SENSITIVE_FIELDS with "***REDACTED***".
    This is a defense-in-depth measure — calling code should already
    avoid logging sensitive data, but this catches accidental leaks.
    """
    for key in list(event_dict.keys()):
        if key.lower() in SENSITIVE_FIELDS:
            event_dict[key] = "***REDACTED***"
    return event_dict


def _configure_text_logging(level: int) -> None:
    """Human-readable text logs — development."""
    logging.basicConfig(
        level=level,
        stream=sys.stdout,
        format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )
