# WASLNI — Monitoring & Logging

> Crash reporting, structured logging, and uptime monitoring for the WASLNI app.

## Android — Firebase Crashlytics

### Setup
1. Create a Firebase project at https://console.firebase.google.com
2. Add an Android app with package `com.waslni.driver`
3. Download `google-services.json` → place at `android/app/google-services.json`
4. Enable Crashlytics in Firebase Console → Analytics → Crashlytics

### What's Tracked

| Event | Trigger | Data Captured |
|-------|---------|---------------|
| Crash (unhandled exception) | Global UncaughtExceptionHandler | Stack trace, thread, custom keys |
| Handled exception | `crashReporter.reportException()` | Stack trace, message, custom keys |
| Breadcrumb | `crashReporter.logEvent()` | Event name + params |
| User ID | On login (`setUserId`) | User UUID (not PII) |
| User logout | `clearUserId()` | Clears user association |

### Custom Keys
```kotlin
crashReporter.reportException(
    exception = e,
    message = "Failed to sync customer",
    customKeys = mapOf(
        "operation" to "CREATE_CUSTOMER",
        "retry_count" to "3"
    )
)
```

### Events Logged
- `login_success` — after successful login
- `logout` — on logout
- `delivery_completed` — on delivery completion
- `delivery_cancelled` — on delivery cancellation
- `sync_performed` — after each sync batch

### Privacy
- User ID is the UUID (not username/phone)
- No tokens, passwords, or phone numbers in crash reports
- GPS coordinates are never logged
- `SENSITIVE_FIELDS` redaction processor on backend catches accidental leaks

---

## Backend — Structured Logging (structlog)

### Configuration
- **Production**: JSON format, `LOG_LEVEL=WARNING`
- **Development**: text format, `LOG_LEVEL=DEBUG`
- **Staging**: JSON format, `LOG_LEVEL=INFO`

### JSON Log Format
```json
{
  "event": "login_success",
  "level": "info",
  "timestamp": "2026-09-08T10:30:00Z",
  "user_id": "550e8400-e29b-41d4-a716-446655440000",
  "ip": "1.2.3.4"
}
```

### Sensitive Field Redaction
The `_redact_sensitive_fields` structlog processor automatically replaces:
- `password`, `password_hash`
- `access_token`, `refresh_token`
- `jwt_secret`, `token`, `authorization`
- `phone`, `latitude`, `longitude`, `accuracy`

with `"***REDACTED***"`. This is defense-in-depth — calling code should
already avoid logging sensitive data.

### Usage
```python
from app.core.logging import get_logger

logger = get_logger(__name__)
logger.info("login_success", user_id=user.id, ip=ip_address)
logger.warning("slow_query", query="get_customer_by_id", elapsed_ms=150)
logger.error("sync_failed", operation_id=op_id, error=str(e))
```

---

## Uptime Monitoring

### Endpoints

| Endpoint | Auth | Purpose |
|----------|------|---------|
| `GET /health` | ❌ | Liveness + DB readiness (Docker healthcheck) |
| `GET /api/v1/health` | ❌ | V1 liveness |
| `GET /api/v1/metrics` | ❌ | System metrics (uptime, DB latency, env) |

### /metrics Response
```json
{
  "status": "ok",
  "db": "ok",
  "db_latency_ms": 2.34,
  "env": "production",
  "version": "1.0.0",
  "uptime_seconds": 86400.5,
  "timestamp": "2026-09-08T10:30:00Z"
}
```

### External Monitoring Setup
Configure UptimeRobot (or BetterStack / Pingdom):
1. URL: `https://api.waslni.com/api/v1/metrics`
2. Check interval: 1 minute
3. Expected status: 200
4. Expected content: `"status": "ok"`
5. Alert on: status != 200 OR `db` != `"ok"` OR `db_latency_ms` > 500

### Alert Channels
- Email: ops@waslni.com
- Slack/Discord webhook (future)
- SMS for critical downtime (future)

---

## Log Aggregation (Future)

### Backend Logs → Log Collector
```yaml
# docker-compose.prod.yml — backend logs → journald or file
backend:
  logging:
    driver: json-file
    options:
      max-size: "10m"
      max-file: "5"
```

Collect with:
- **Loki + Grafana** (recommended for small teams)
- **Elasticsearch + Kibana** (ELK stack — heavier)
- **Cloud provider's log service** (AWS CloudWatch, GCP Logging)

### Log Rotation
```bash
# /etc/logrotate.d/waselni
/opt/waselni/logs/nginx/*.log {
    daily
    rotate 14
    compress
    delaycompress
    missingok
    notifempty
    create 644 waselni waselni
}
```

---

## Monitoring Checklist

| Item | Status | Notes |
|------|--------|-------|
| Firebase Crashlytics enabled | ✅ | CrashReporter wired to Crashlytics |
| Global uncaught exception handler | ✅ | WaselApp.onCreate() |
| User ID correlation | ✅ | setUserId on login, clearUserId on logout |
| Event breadcrumbs | ✅ | login, logout, delivery, sync events |
| Structured JSON logging | ✅ | structlog with JSONRenderer |
| Sensitive field redaction | ✅ | _redact_sensitive_fields processor |
| /health endpoint | ✅ | Docker healthcheck |
| /metrics endpoint | ✅ | Uptime + DB latency |
| Uptime monitoring (UptimeRobot) | ☐ | Configure after deployment |
| Log rotation | ✅ | logrotate config documented |
| Log aggregation (Loki/Grafana) | ☐ | Post-MVP |
| ANR detection | ☐ | Firebase Crashlytics ANR (automatic) |
| Performance monitoring | ☐ | Firebase Performance (future) |
