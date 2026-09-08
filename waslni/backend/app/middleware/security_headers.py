"""Security headers middleware.

Adds HTTP headers that protect against common web vulnerabilities:
  - X-Content-Type-Options: nosniff → prevents MIME-type sniffing.
  - X-Frame-Options: DENY → prevents clickjacking.
  - X-XSS-Protection: 0 → disabled (modern browsers use CSP instead;
    we set 0 to avoid the legacy filter's bugs).
  - Strict-Transport-Security → enforces HTTPS (only sent in production).
  - Referrer-Policy: no-referrer → don't leak the API URL in referrer headers.
  - Cache-Control: no-store → prevent caching of API responses (they contain
    sensitive data like tokens and customer info).

These headers are defense-in-depth — they don't replace proper auth + validation,
but they make the API a harder target.
"""
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response

from app.core.config import settings


class SecurityHeadersMiddleware(BaseHTTPMiddleware):
    """Adds security-related HTTP headers to every response."""

    async def dispatch(self, request: Request, call_next):
        response: Response = await call_next(request)

        # Always-on headers
        response.headers["X-Content-Type-Options"] = "nosniff"
        response.headers["X-Frame-Options"] = "DENY"
        response.headers["X-XSS-Protection"] = "0"
        response.headers["Referrer-Policy"] = "no-referrer"
        response.headers["Cache-Control"] = "no-store, no-cache, must-revalidate"
        response.headers["Pragma"] = "no-cache"
        response.headers["Expires"] = "0"

        # HSTS only in production (behind HTTPS reverse proxy)
        if settings.is_production:
            response.headers["Strict-Transport-Security"] = (
                "max-age=63072000; includeSubDomains; preload"
            )

        return response
