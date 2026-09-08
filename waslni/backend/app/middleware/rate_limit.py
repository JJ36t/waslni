"""Rate limiting middleware.

In-memory sliding-window rate limiter. Production should use Redis
(or a managed rate limiter), but this is sufficient for single-instance
staging/dev and for the MVP.

Limits:
  - Global: 100 requests per minute per authenticated user (or IP if unauthenticated).
  - Login: 5 attempts per minute per IP (stricter — brute-force protection).

The middleware sets X-RateLimit-* headers on every response so the client
can self-throttle.
"""
import time
from collections import defaultdict
from typing import Any

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import JSONResponse, Response

from app.core.config import settings
from app.core.exceptions import ErrorCodes, format_error_response


class RateLimitMiddleware(BaseHTTPMiddleware):
    """In-memory sliding-window rate limiter."""

    def __init__(self, app: Any) -> None:
        super().__init__(app)
        # key: (identifier, path_pattern) → list of timestamps
        self._windows: dict[tuple[str, str], list[float]] = defaultdict(list)
        self._window_seconds = 60.0

    async def dispatch(self, request: Request, call_next):
        # Skip rate limiting for health checks
        path = request.url.path
        if path.endswith("/health"):
            return await call_next(request)

        # Determine the identifier: prefer authenticated user ID, fall back to IP
        identifier = self._get_identifier(request)

        # Determine the limit: stricter for login
        is_login = path.endswith("/auth/login")
        limit = settings.LOGIN_RATE_LIMIT_PER_MINUTE if is_login else settings.RATE_LIMIT_PER_MINUTE
        pattern = "login" if is_login else "global"

        # Check + record
        key = (identifier, pattern)
        now = time.monotonic()
        cutoff = now - self._window_seconds

        # Purge old entries
        self._windows[key] = [t for t in self._windows[key] if t > cutoff]

        # Check limit
        if len(self._windows[key]) >= limit:
            response = JSONResponse(
                status_code=429,
                content=format_error_response(
                    code=ErrorCodes.RATE_LIMIT_EXCEEDED,
                    message="Too many requests. Try again in a minute.",
                ),
            )
            self._add_rate_limit_headers(response, limit, 0, int(self._window_seconds))
            return response

        # Record this request
        self._windows[key].append(now)

        # Process the request
        response: Response = await call_next(request)

        # Add rate limit headers
        remaining = max(0, limit - len(self._windows[key]))
        self._add_rate_limit_headers(response, limit, remaining, int(self._window_seconds))

        return response

    def _get_identifier(self, request: Request) -> str:
        """Extract the rate-limit identifier: user ID from JWT, or client IP."""
        # Try to extract user ID from the Authorization header
        # (We don't fully decode the JWT here — just extract the subject.
        #  The full auth check happens in the endpoint dependency.)
        auth = request.headers.get("Authorization", "")
        if auth.startswith("Bearer "):
            try:
                from app.core.security import decode_token

                payload = decode_token(auth.removeprefix("Bearer "))
                return payload.get("sub", "unknown")
            except Exception:
                pass  # Invalid token — fall back to IP

        # Fall back to client IP
        forwarded = request.headers.get("X-Forwarded-For")
        if forwarded:
            return forwarded.split(",")[0].strip()
        if request.client:
            return request.client.host
        return "unknown"

    def _add_rate_limit_headers(
        self, response: Response, limit: int, remaining: int, reset: int
    ) -> None:
        response.headers["X-RateLimit-Limit"] = str(limit)
        response.headers["X-RateLimit-Remaining"] = str(remaining)
        response.headers["X-RateLimit-Reset"] = str(reset)
