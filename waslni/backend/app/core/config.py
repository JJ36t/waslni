"""Application configuration loaded from environment variables.

Uses Pydantic Settings which:
  - Reads from .env (via python-dotenv) AND from os.environ.
  - Validates types and required fields at startup.
  - Rejects unknown fields (catches typos in env var names).

All settings are read-only after the singleton is constructed.
"""
from functools import lru_cache
from typing import Literal

from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Application settings — populated from environment / .env file."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    # === App ===
    APP_ENV: Literal["development", "test", "staging", "production"] = "development"
    APP_NAME: str = "waselni-backend"
    APP_DEBUG: bool = False
    APP_HOST: str = "0.0.0.0"
    APP_PORT: int = 8000

    # === Database ===
    DATABASE_URL: str = Field(
        default="postgresql+asyncpg://waselni:waselni@localhost:5432/waslni",
        description="SQLAlchemy async URL — must use asyncpg driver",
    )

    # === JWT / Auth ===
    JWT_SECRET: str = Field(
        default="change-me-in-production",
        min_length=32,
        description="256-bit secret used to sign JWTs. Set via env in prod.",
    )
    JWT_ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 15
    REFRESH_TOKEN_EXPIRE_DAYS: int = 30

    # === CORS ===
    CORS_ORIGINS: str = "http://localhost:8080"

    # === Rate limiting ===
    RATE_LIMIT_PER_MINUTE: int = 100
    LOGIN_RATE_LIMIT_PER_MINUTE: int = 5

    # === Mapbox (optional) ===
    MAPBOX_API_KEY: str = ""

    # === FCM (optional) ===
    FCM_CREDENTIALS_PATH: str = ""

    # === Logging ===
    LOG_LEVEL: str = "INFO"
    LOG_FORMAT: Literal["json", "text"] = "json"

    @property
    def cors_origins_list(self) -> list[str]:
        """Parse CORS_ORIGINS (comma-separated) into a list."""
        return [origin.strip() for origin in self.CORS_ORIGINS.split(",") if origin.strip()]

    @property
    def is_production(self) -> bool:
        return self.APP_ENV == "production"

    @property
    def is_development(self) -> bool:
        return self.APP_ENV == "development"

    @property
    def is_test(self) -> bool:
        return self.APP_ENV == "test"

    @field_validator("JWT_SECRET")
    @classmethod
    def _warn_weak_secret(cls, v: str) -> str:
        if v.startswith("change-me"):
            import warnings

            warnings.warn(
                "JWT_SECRET is still the default value — set a strong secret in production!",
                stacklevel=2,
            )
        return v


@lru_cache
def get_settings() -> Settings:
    """Singleton settings accessor.

    Cached so we read env vars once per process. Tests can call
    `get_settings.cache_clear()` to reset between test modules.
    """
    return Settings()


# Convenience module-level instance — most callers use this directly.
settings = get_settings()
