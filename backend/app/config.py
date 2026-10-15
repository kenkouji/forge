import os
from typing import List, Union
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import field_validator

class Settings(BaseSettings):
    # Network Configuration (Server-Agnostic: binds to 0.0.0.0)
    SERVER_HOST: str = "0.0.0.0"
    SERVER_PORT: int = 8000
    SERVER_PUBLIC_URL: str = "http://localhost:8000"

    # Database
    DATABASE_URL: str = "sqlite:///./remoteforge.db"

    # Security & JWT Auth
    JWT_SECRET: str = "change-this-to-a-super-secret-random-32-char-key-in-production"
    JWT_ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 1440  # 24 hours

    # Initial Admin Credentials (Created automatically if no user exists)
    INITIAL_ADMIN_USERNAME: str = "admin"
    INITIAL_ADMIN_PASSWORD: str = "remoteforge_secure_pass"

    # PC Agent Authentication Token
    AGENT_TOKEN: str = "rf_agent_secret_token_default_replace_me"

    # NetBird / CORS Settings
    NETBIRD_SERVER_ADDRESS: str = ""
    CORS_ORIGINS: Union[List[str], str] = ["*"]

    # Optional AI Engine (Phase 4)
    AI_PROVIDER: str = ""
    AI_API_KEY: str = ""

    @field_validator("CORS_ORIGINS", mode="before")
    @classmethod
    def assemble_cors_origins(cls, v: Union[str, List[str]]) -> List[str]:
        if isinstance(v, str):
            if v.startswith("[") and v.endswith("]"):
                import json
                try:
                    return json.loads(v)
                except Exception:
                    pass
            return [i.strip() for i in v.split(",") if i.strip()]
        return v

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )

settings = Settings()
