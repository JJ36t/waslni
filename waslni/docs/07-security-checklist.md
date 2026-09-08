# WASLNI — Security Checklist

> Pre-release security audit. All items must be checked before MVP launch.

## ✅ Transport Security

| Item | Status | Notes |
|------|--------|-------|
| HTTPS enforced in production | ✅ | API_BASE_URL = `https://api.waslni.com/api/v1/` in release build |
| HSTS header in production | ✅ | `SecurityHeadersMiddleware` adds `Strict-Transport-Security: max-age=63072000; includeSubDomains; preload` |
| Certificate pinning | ✅ | `network_security_config.xml` pins Let's Encrypt ISRG Root X1 + X2 for `api.waslni.com` |
| Cleartext traffic disabled in release | ✅ | Network security config only allows cleartext for `10.0.2.2` (emulator dev) |
| Debug CAs trusted only in debug | ✅ | `<debug-overrides>` trusts user CAs only in debug builds |

## ✅ Authentication & Authorization

| Item | Status | Notes |
|------|--------|-------|
| Passwords hashed with Argon2id | ✅ | `passlib.CryptContext` with 64MB/3/4 params |
| JWT access tokens (15 min expiry) | ✅ | HS256, `exp` + `iat` + `sub` + `type` |
| JWT refresh tokens (30 day expiry) | ✅ | Stored as SHA-256 hashes in DB (not plaintext) |
| Refresh token rotation | ✅ | Old token revoked on every refresh |
| Refresh token reuse detection | ✅ | Reuse → revoke ALL user tokens (suspected theft) |
| Token type enforcement | ✅ | Access tokens rejected by `/auth/refresh`, refresh tokens rejected by `/auth/me` |
| Authorization scoped by driver_id | ✅ | All customer/delivery queries include `WHERE driver_id = :current_user_id` |
| Same error for "not found" and "not yours" | ✅ | 404 for both — no information leak |
| Rate limiting on login | ✅ | 5 attempts/min per IP via `RateLimitMiddleware` |
| Rate limiting on all endpoints | ✅ | 100 requests/min per user via `RateLimitMiddleware` |

## ✅ Token Storage (Android)

| Item | Status | Notes |
|------|--------|-------|
| Tokens in EncryptedSharedPreferences | ✅ | AES-256-GCM, Keystore-backed master key |
| No tokens in SharedPreferences (plain) | ✅ | All token access goes through `SecureStorage` |
| No tokens in logs | ✅ | `HttpLoggingInterceptor.Level.HEADERS` (no body) in debug; no logging in release |
| Tokens cleared on logout | ✅ | `TokenManager.clearSession()` wipes all auth fields |
| User preferences cleared on logout | ✅ | `UserPreferences.clear()` wipes theme + thresholds |

## ✅ Input Validation

| Item | Status | Notes |
|------|--------|-------|
| Pydantic validation on all endpoints | ✅ | All request bodies use Pydantic models with field constraints |
| Latitude -90..90, longitude -180..180 | ✅ | `Field(ge=-90, le=90)` / `Field(ge=-180, le=180)` |
| Name 2-120 chars, phone 7-30 chars | ✅ | `Field(min_length=2, max_length=120)` etc. |
| Accuracy >= 0 | ✅ | `Field(ge=0)` |
| SQL injection protection | ✅ | SQLAlchemy ORM uses parameterized queries (verified by `test_security_audit.py`) |
| Phone normalization | ✅ | Spaces/dashes/parens stripped on create + update |

## ✅ API Security

| Item | Status | Notes |
|------|--------|-------|
| Security headers on all responses | ✅ | `SecurityHeadersMiddleware` adds X-Content-Type-Options, X-Frame-Options, Cache-Control, Referrer-Policy |
| Cache-Control: no-store | ✅ | Prevents caching of API responses containing tokens/customer data |
| CORS restricted to known origins | ✅ | `settings.cors_origins_list` from env, not `*` |
| OpenAPI docs disabled in production | ✅ | `docs_url=None` when `settings.is_production` |
| Error responses never leak internals | ✅ | `format_error_response()` returns code + message only; SQLAlchemyError → generic 500 |
| No secrets in source code | ✅ | JWT_SECRET, MAPBOX_API_KEY from env vars; `.env` in `.gitignore` |
| Idempotency-Key for critical operations | ✅ | PATCH /deliveries/{id}/status prevents double-execution on retry |
| Delivery state machine enforced | ✅ | Backend `_TRANSITIONS` dict + DB CHECK constraint (defense in depth) |

## ✅ Data Privacy

| Item | Status | Notes |
|------|--------|-------|
| Minimal data collection | ✅ | No address, no description — only name + phone + GPS |
| Phone numbers not in logs | ✅ | Structured logging excludes sensitive fields |
| GPS coordinates not in logs | ✅ | Only audit logs store entity_id, never coordinates |
| Audit logs don't store tokens/passwords | ✅ | `AuditLogRepository.record()` doc explicitly prohibits sensitive metadata |
| Backup excludes encrypted prefs + Room DB | ✅ | `backup_rules.xml` + `data_extraction_rules.xml` exclude `encrypted_prefs.xml` and `waselni.db` |
| Account deletion (future) | 🔲 | Phase 32+ — GDPR right-to-erasure |

## ✅ Android Build Security

| Item | Status | Notes |
|------|--------|-------|
| ProGuard/R8 enabled in release | ✅ | `isMinifyEnabled = true`, `isShrinkResources = true` |
| ProGuard keep rules for Hilt/Room/Retrofit/Serialization | ✅ | `proguard-rules.pro` covers all codegen libraries |
| Debug flag off in release | ✅ | `isDebuggable = false` (default for release build type) |
| Signing config from env vars (not in git) | ✅ | `WASLNI_KEYSTORE_FILE` etc. from env/local.properties |
| `applicationIdSuffix = ".debug"` for debug builds | ✅ | Prevents debug/release install conflict |
| No `android:debuggable` in Manifest | ✅ | Controlled by build type, not Manifest |
| `android:allowBackup` excludes sensitive data | ✅ | `backup_rules.xml` excludes encrypted prefs + Room DB |

## ✅ Backend Deployment Security

| Item | Status | Notes |
|------|--------|-------|
| Non-root Docker user | ✅ | `USER waselni` (uid 1000) in Dockerfile |
| Docker healthcheck | ✅ | `curl -fsS http://localhost:8000/health` |
| Database credentials from env | ✅ | `DATABASE_URL` from `.env` |
| Separate dev/staging/prod environments | ✅ | `APP_ENV` controls behavior; separate DBs |
| Alembic migrations (no manual schema changes) | ✅ | All schema changes via `alembic revision` |
| Automated DB backups (planned) | 🔲 | Phase 25 — daily/weekly/retention |
| Dependency audit | 🔲 | Phase 22 — `pip-audit` + `npm audit` |

## ✅ Security Testing

| Item | Status | Notes |
|------|--------|-------|
| Cross-driver access tests | ✅ | `test_security_audit.py::TestCrossDriverAccess` — 3 tests |
| Token tampering tests | ✅ | `test_security_audit.py::TestTokenTampering` — 5 tests |
| SQL injection tests | ✅ | `test_security_audit.py::TestSQLInjection` — 2 tests |
| Input validation tests | ✅ | `test_security_audit.py::TestInputValidation` — 5 tests |
| No sensitive data leakage tests | ✅ | `test_security_audit.py::TestNoSensitiveDataLeakage` — 3 tests |
| Security headers tests | ✅ | `test_security_audit.py::TestSecurityHeaders` — 4 tests |

## 🔲 Post-Launch Security (Phase 28+)

| Item | Status |
|------|--------|
| Sentry / Crashlytics for error monitoring | 🔲 |
| Dependency vulnerability scanning (pip-audit) | 🔲 |
| Regular JWT_SECRET rotation policy | 🔲 |
| Penetration testing | 🔲 |
| Rate limit via Redis (not in-memory) | 🔲 |
| Web Application Firewall (WAF) | 🔲 |
