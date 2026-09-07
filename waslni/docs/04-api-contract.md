# وصلني (WASLNI) — API Contract (OpenAPI)

> **النسخة:** v1
> **Base URL:** `https://api.waslni.com/api/v1`
> **التاريخ:** 2026-09-08
> **المرحلة:** Planning

---

## 1. نظرة عامة (Overview)

### 1.1 Base URL & Versioning
- كل endpoints تحت `/api/v1/`.
- الإصدار القادم سيكون `/api/v2/` عند الحاجة لـ breaking changes.

### 1.2 Content Type
- **Request:** `application/json`
- **Response:** `application/json`
- **Encoding:** UTF-8

### 1.3 Authentication
- **Scheme:** Bearer Token (JWT)
- **Header:** `Authorization: Bearer <access_token>`
- **Endpoints معروفة من غير auth:** `/auth/login`, `/auth/refresh`
- **كل الباقي:** يتطلب Access Token صالح.

---

## 2. Unified Error Model

كل الأخطاء ترجع بنفس الشكل:

```json
{
  "error": {
    "code": "CUSTOMER_NOT_FOUND",
    "message": "Customer not found",
    "details": {
      "customer_id": "abc-123"
    }
  }
}
```

### 2.1 Error Codes

| Code | HTTP | الوصف |
|------|------|------|
| `INVALID_CREDENTIALS` | 401 | اسم المستخدم/كلمة المرور غير صحيحة |
| `TOKEN_EXPIRED` | 401 | انتهت صلاحية الـ token |
| `TOKEN_INVALID` | 401 | الـ token غير صالح |
| `REFRESH_TOKEN_INVALID` | 401 | refresh token غير صالح أو منتهي |
| `UNAUTHORIZED` | 401 | لا يوجد token |
| `FORBIDDEN` | 403 | لا تملك الصلاحية |
| `ACCOUNT_DISABLED` | 403 | الحساب معطل |
| `RESOURCE_NOT_FOUND` | 404 | المورد غير موجود |
| `CUSTOMER_NOT_FOUND` | 404 | الزبون غير موجود |
| `DELIVERY_NOT_FOUND` | 404 | التوصيل غير موجود |
| `VALIDATION_ERROR` | 422 | فشل التحقق من البيانات |
| `DUPLICATE_PHONE` | 409 | الرقم مستخدم مسبقًا |
| `INVALID_STATE_TRANSITION` | 409 | انتقال حالة غير صالح |
| `RATE_LIMIT_EXCEEDED` | 429 | تجاوز حد الطلبات |
| `IDEMPOTENCY_CONFLICT` | 409 | idempotency-key لا يطابق request |
| `INTERNAL_ERROR` | 500 | خطأ داخلي في السيرفر |
| `SERVICE_UNAVAILABLE` | 503 | الخدمة غير متاحة |

### 2.2 Validation Error Format

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": {
      "fields": [
        { "field": "name", "message": "name must be at least 2 characters" },
        { "field": "latitude", "message": "latitude must be between -90 and 90" }
      ]
    }
  }
}
```

---

## 3. Endpoints Overview

| Method | Endpoint | Auth | الوصف |
|--------|---------|------|------|
| POST | `/auth/login` | ❌ | تسجيل الدخول |
| POST | `/auth/refresh` | ❌ (refresh token) | تجديد access token |
| POST | `/auth/logout` | ✅ | تسجيل الخروج |
| GET | `/auth/me` | ✅ | معلومات المستخدم الحالي |
| GET | `/customers` | ✅ | قائمة الزبائن (paginated) |
| POST | `/customers` | ✅ | إضافة زبون |
| GET | `/customers/{id}` | ✅ | تفاصيل زبون |
| PATCH | `/customers/{id}` | ✅ | تعديل زبون |
| DELETE | `/customers/{id}` | ✅ | حذف زبون |
| GET | `/deliveries` | ✅ | قائمة التوصيلات |
| POST | `/deliveries` | ✅ | إنشاء توصيل |
| GET | `/deliveries/{id}` | ✅ | تفاصيل توصيل |
| PATCH | `/deliveries/{id}/status` | ✅ | تحديث حالة التوصيل |
| POST | `/sync` | ✅ | دفعة مزامنة (batch) |

---

## 4. Authentication Endpoints

### 4.1 POST `/auth/login`

**Description:** تسجيل الدخول بـ username + password.

**Request:**
```json
{
  "username": "driver_01",
  "password": "secret123"
}
```

**Response (200):**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIs...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIs...",
  "token_type": "bearer",
  "expires_in": 900,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "driver_01",
    "role": "driver"
  }
}
```

**Errors:**
- 401 `INVALID_CREDENTIALS`
- 403 `ACCOUNT_DISABLED`
- 422 `VALIDATION_ERROR`
- 429 `RATE_LIMIT_EXCEEDED`

---

### 4.2 POST `/auth/refresh`

**Description:** تجديد access token باستخدام refresh token.

**Request:**
```json
{
  "refresh_token": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response (200):**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIs...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIs...",
  "token_type": "bearer",
  "expires_in": 900
}
```

**Errors:**
- 401 `REFRESH_TOKEN_INVALID`

---

### 4.3 POST `/auth/logout`

**Description:** تسجيل الخروج وإبطال refresh token.

**Headers:** `Authorization: Bearer <access_token>`

**Request:**
```json
{
  "refresh_token": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response (204):** No content

---

### 4.4 GET `/auth/me`

**Headers:** `Authorization: Bearer <access_token>`

**Response (200):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "driver_01",
  "role": "driver",
  "is_active": true,
  "last_login_at": "2026-09-08T10:30:00Z"
}
```

---

## 5. Customers Endpoints

### 5.1 GET `/customers`

**Query Parameters:**
| Param | Type | Default | الوصف |
|-------|------|---------|------|
| `page` | int | 1 | رقم الصفحة |
| `limit` | int | 50 | عدد العناصر (max 200) |
| `search` | string | null | بحث في name/phone |

**Response (200):**
```json
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "محمد أحمد",
      "phone": "07801234567",
      "latitude": 31.9789420,
      "longitude": 44.9401270,
      "accuracy": 4.2,
      "created_at": "2026-09-08T10:30:00Z",
      "updated_at": "2026-09-08T10:30:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 50,
    "total": 248,
    "total_pages": 5
  }
}
```

---

### 5.2 POST `/customers`

**Request:**
```json
{
  "name": "محمد أحمد",
  "phone": "07801234567",
  "latitude": 31.9789420,
  "longitude": 44.9401270,
  "accuracy": 4.2
}
```

**Response (201):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "محمد أحمد",
  "phone": "07801234567",
  "latitude": 31.9789420,
  "longitude": 44.9401270,
  "accuracy": 4.2,
  "created_at": "2026-09-08T10:30:00Z",
  "updated_at": "2026-09-08T10:30:00Z"
}
```

**Errors:**
- 409 `DUPLICATE_PHONE` — الرقم مستخدم مسبقًا عند نفس المندوب.
- 422 `VALIDATION_ERROR`

**Validation:**
- `name`: 2-120 chars
- `phone`: 7-30 chars
- `latitude`: -90 to 90
- `longitude`: -180 to 180
- `accuracy`: ≥ 0, nullable

---

### 5.3 GET `/customers/{id}`

**Response (200):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "محمد أحمد",
  "phone": "07801234567",
  "latitude": 31.9789420,
  "longitude": 44.9401270,
  "accuracy": 4.2,
  "created_at": "2026-09-08T10:30:00Z",
  "updated_at": "2026-09-08T10:30:00Z"
}
```

**Errors:**
- 404 `CUSTOMER_NOT_FOUND`
- 403 `FORBIDDEN` — إذا الزبون يخص مندوب آخر.

---

### 5.4 PATCH `/customers/{id}`

**Request (partial update):**
```json
{
  "name": "محمد علي أحمد",
  "phone": "07801234567"
}
```

أو لتحديث الموقع فقط:
```json
{
  "latitude": 31.978950,
  "longitude": 44.940120,
  "accuracy": 6.0
}
```

**Response (200):** مثل GET response.

**Errors:**
- 404 `CUSTOMER_NOT_FOUND`
- 409 `DUPLICATE_PHONE`
- 422 `VALIDATION_ERROR`

> **ملاحظة:** الـ Backend يحدّث `updated_at` تلقائيًا. لا يجوز للمندوب تعيينه يدويًا (السيرفر هو المصدر).

---

### 5.5 DELETE `/customers/{id}`

**Response (204):** No content

**Errors:**
- 404 `CUSTOMER_NOT_FOUND`
- 409 `CUSTOMER_HAS_ACTIVE_DELIVERIES` — لا يمكن حذف زبون لديه توصيل نشط.

---

## 6. Deliveries Endpoints

### 6.1 GET `/deliveries`

**Query Parameters:**
| Param | Type | الوصف |
|-------|------|------|
| `page` | int | رقم الصفحة |
| `limit` | int | عدد العناصر |
| `status` | string | فلتر بالحالة |
| `from_date` | date | من تاريخ (YYYY-MM-DD) |
| `to_date` | date | إلى تاريخ |

**Response (200):**
```json
{
  "data": [
    {
      "id": "660e8400-e29b-41d4-a716-446655440001",
      "customer_id": "550e8400-e29b-41d4-a716-446655440000",
      "customer": {
        "id": "550e8400-...",
        "name": "محمد أحمد",
        "phone": "07801234567",
        "latitude": 31.9789420,
        "longitude": 44.9401270
      },
      "status": "DELIVERED",
      "created_at": "2026-09-08T10:30:00Z",
      "started_at": "2026-09-08T10:35:00Z",
      "arrived_at": "2026-09-08T10:44:00Z",
      "completed_at": "2026-09-08T10:45:00Z",
      "cancelled_at": null
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 50,
    "total": 12,
    "total_pages": 1
  }
}
```

---

### 6.2 POST `/deliveries`

**Request:**
```json
{
  "customer_id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ON_THE_WAY"
}
```

> عند إنشاء توصيل من Android، الـ status الافتراضي `ON_THE_WAY` مع `started_at = now()`.

**Response (201):**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "customer_id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ON_THE_WAY",
  "created_at": "2026-09-08T10:35:00Z",
  "started_at": "2026-09-08T10:35:00Z",
  "arrived_at": null,
  "completed_at": null,
  "cancelled_at": null
}
```

**Errors:**
- 404 `CUSTOMER_NOT_FOUND`
- 409 `CUSTOMER_HAS_ACTIVE_DELIVERY` — يوجد توصيل نشط لهذا الزبون.
- 422 `VALIDATION_ERROR`

---

### 6.3 GET `/deliveries/{id}`

**Response (200):** مثل عنصر في قائمة `/deliveries`.

---

### 6.4 PATCH `/deliveries/{id}/status`

**Request:**
```json
{
  "status": "DELIVERED"
}
```

> **مهم:** يجب إرسال `Idempotency-Key` header لهذا الـ endpoint لمنع التكرار.

**Headers:**
```
Idempotency-Key: 770e8400-e29b-41d4-a716-446655440002
```

**Response (200):**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "status": "DELIVERED",
  "completed_at": "2026-09-08T10:45:00Z",
  "started_at": "2026-09-08T10:35:00Z",
  "arrived_at": "2026-09-08T10:44:00Z"
}
```

**Errors:**
- 404 `DELIVERY_NOT_FOUND`
- 409 `INVALID_STATE_TRANSITION` — انتقال غير صالح (مثل `PENDING → DELIVERED`).
- 409 `IDEMPOTENCY_CONFLICT` — نفس key مع request مختلف.

**Valid Transitions:**
```
PENDING    → ASSIGNED | CANCELLED
ASSIGNED   → ON_THE_WAY | CANCELLED
ON_THE_WAY → ARRIVED | CANCELLED
ARRIVED    → DELIVERED | CANCELLED
DELIVERED  → (none - terminal)
CANCELLED  → (none - terminal)
```

---

## 7. Sync Endpoint

### 7.1 POST `/sync`

**Description:** يقبل دفعة من العمليات المعلقة (pending operations) من Android ويرجع نتائج كل واحدة.

**Headers:**
```
Authorization: Bearer <access_token>
Idempotency-Key: <uuid>   (optional - للدفعة ككل)
```

**Request:**
```json
{
  "operations": [
    {
      "id": "op-001",
      "entity_type": "CUSTOMER",
      "operation": "CREATE",
      "payload": {
        "id": "550e8400-...",
        "name": "محمد أحمد",
        "phone": "07801234567",
        "latitude": 31.9789420,
        "longitude": 44.9401270,
        "accuracy": 4.2,
        "created_at": "2026-09-08T10:30:00Z",
        "updated_at": "2026-09-08T10:30:00Z"
      }
    },
    {
      "id": "op-002",
      "entity_type": "DELIVERY",
      "operation": "COMPLETE",
      "payload": {
        "id": "660e8400-...",
        "customer_id": "550e8400-...",
        "completed_at": "2026-09-08T10:45:00Z"
      },
      "idempotency_key": "770e8400-..."
    },
    {
      "id": "op-003",
      "entity_type": "CUSTOMER",
      "operation": "UPDATE",
      "payload": {
        "id": "550e8400-...",
        "name": "محمد علي أحمد",
        "updated_at": "2026-09-08T11:00:00Z"
      }
    },
    {
      "id": "op-004",
      "entity_type": "CUSTOMER",
      "operation": "DELETE",
      "payload": {
        "id": "550e8400-..."
      }
    }
  ]
}
```

**Response (200):**
```json
{
  "results": [
    {
      "operation_id": "op-001",
      "status": "SUCCESS",
      "entity_id": "550e8400-...",
      "server_state": {
        "id": "550e8400-...",
        "name": "محمد أحمد",
        "updated_at": "2026-09-08T10:30:00Z"
      }
    },
    {
      "operation_id": "op-002",
      "status": "SUCCESS",
      "entity_id": "660e8400-..."
    },
    {
      "operation_id": "op-003",
      "status": "CONFLICT",
      "error": {
        "code": "STALE_UPDATE",
        "message": "Server has newer version of this customer",
        "server_state": {
          "id": "550e8400-...",
          "updated_at": "2026-09-08T11:30:00Z"
        }
      }
    },
    {
      "operation_id": "op-004",
      "status": "FAILED",
      "error": {
        "code": "CUSTOMER_HAS_ACTIVE_DELIVERIES",
        "message": "Cannot delete customer with active deliveries"
      }
    }
  ],
  "server_changes": [
    {
      "entity_type": "DELIVERY",
      "entity_id": "660e8400-...",
      "operation": "UPDATE",
      "payload": { ... }
    }
  ],
  "latest_sync_timestamp": "2026-09-08T11:30:00Z"
}
```

### 7.2 Operation Statuses (in response)

| Status | الوصف |
|--------|------|
| `SUCCESS` | تمت بنجاح |
| `CONFLICT` | تعارض — راجع `server_state` |
| `FAILED` | فشل — راجع `error` |
| `IGNORED` | تجاهل (مثل: DELETE لزبون محذوف مسبقًا) |

### 7.3 Server Changes (download direction)

عند الـ sync، السيرفر يرجع أي تغييرات حدثت منذ آخر `latest_sync_timestamp` على مستوى المندوب. هذا يسمح بـ:
- المندوب A يعدّل زبون من جهاز آخر → الجهاز الحالي يستلم التحديث.
- Admin يعطّل حساب → التطبيق يطلب logout.

---

## 8. Pagination Convention

كل endpoints للقوائم ترجع:
```json
{
  "data": [ ... ],
  "pagination": {
    "page": 1,
    "limit": 50,
    "total": 248,
    "total_pages": 5,
    "has_next": true,
    "has_prev": false
  }
}
```

---

## 9. Status Codes Summary

| Code | المعنى |
|------|------|
| 200 OK | نجاح |
| 201 Created | تم الإنشاء |
| 204 No Content | نجاح بدون محتوى |
| 400 Bad Request | طلب غير صالح |
| 401 Unauthorized | لا يوجد token أو منتهي |
| 403 Forbidden | لا تملك الصلاحية |
| 404 Not Found | غير موجود |
| 409 Conflict | تعارض (تكرار/انتقال غير صالح) |
| 422 Unprocessable Entity | فشل validation |
| 429 Too Many Requests | تجاوز حد المعدل |
| 500 Internal Server Error | خطأ بالسيرفر |
| 503 Service Unavailable | صيانة أو overload |

---

## 10. Rate Limiting

| Endpoint | الحد |
|---------|----|
| `/auth/login` | 5 محاولات / دقيقة لكل IP |
| `/auth/refresh` | 30 / دقيقة لكل user |
| كل الباقي | 100 / دقيقة لكل user |

**Headers:**
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 97
X-RateLimit-Reset: 1694169600
```

عند التجاوز:
```json
{
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Too many requests. Try again in 42 seconds."
  }
}
```

---

## 11. HTTP Headers Convention

### Request Headers (Android → Backend)
```
Authorization: Bearer <access_token>
Content-Type: application/json
Accept-Language: ar
User-Agent: Waselni-Android/1.0.0 (Android 13; Pixel 6)
X-Request-Id: <uuid>   (optional, for tracing)
Idempotency-Key: <uuid>   (للعمليات الحرجة)
```

### Response Headers (Backend → Android)
```
Content-Type: application/json
X-Request-Id: <uuid>
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 97
```

---

## 12. Field Naming Convention

- **JSON fields:** `snake_case` (مثل `created_at`, `customer_id`).
- **JSON booleans:** `true`/`false` (lowercase).
- **JSON null:** `null`.
- **Dates:** ISO 8601 UTC (مثل `2026-09-08T10:30:00Z`).
- **UUIDs:** canonical string form.
- **Coordinates:** decimal degrees as numbers (مثل `31.9789420`).

---

## 13. OpenAPI Schema (FastAPI auto-generated)

عند تشغيل الـ Backend، الـ OpenAPI schema يكون متاحًا على:
- **Swagger UI:** `http://localhost:8000/docs`
- **ReDoc:** `http://localhost:8000/redoc`
- **OpenAPI JSON:** `http://localhost:8000/openapi.json`

---

## 14. Examples — End-to-End Scenarios

### 14.1 إضافة زبون + مزامنة

**Android (offline):**
1. Room: insert CustomerEntity (syncState=PENDING)
2. Room: insert SyncOperationEntity (CREATE_CUSTOMER)
3. UI: يعرض الزبون فورًا

**عند عودة الإنترنت:**
```
POST /sync
{
  "operations": [
    {
      "id": "op-001",
      "entity_type": "CUSTOMER",
      "operation": "CREATE",
      "payload": { ... }
    }
  ]
}
```

**Response:**
```json
{
  "results": [
    {
      "operation_id": "op-001",
      "status": "SUCCESS",
      "entity_id": "550e8400-..."
    }
  ]
}
```

**Android:**
- update CustomerEntity.syncState = SYNCED
- update SyncOperationEntity.status = SYNCED

---

### 14.2 إكمال توصيل (Idempotency)

**Android:**
1. Room: update DeliveryEntity (status=DELIVERED, completed_at=now, syncState=PENDING)
2. Room: insert SyncOperationEntity (COMPLETE_DELIVERY, idempotency_key=UUID)
3. WorkManager: يبدأ المزامنة

```
POST /sync
{
  "operations": [
    {
      "id": "op-001",
      "entity_type": "DELIVERY",
      "operation": "COMPLETE",
      "payload": {
        "id": "660e8400-...",
        "completed_at": "2026-09-08T10:45:00Z"
      },
      "idempotency_key": "770e8400-..."
    }
  ]
}
```

**إذا انقطع الإنترنت قبل وصول الـ response:**

Android يعيد الـ request بنفس `idempotency_key`. السيرفر يكتشف التكرار ويرجع نفس الـ response بدون تنفيذ العملية مرتين.

---

### 14.3 Conflict — تعديل زبون من جهازين

**الجهاز A** يعدّل اسم الزبون (offline):
```
name: "محمد علي أحمد"
updated_at: 2026-09-08T11:00:00Z
```

**الجهاز B** يعدّل نفس الزبون (قبل الجهاز A يزامن):
```
name: "محمد أحمد علي"
updated_at: 2026-09-08T11:30:00Z
```

عند مزامنة الجهاز A:
```json
{
  "results": [
    {
      "operation_id": "op-001",
      "status": "CONFLICT",
      "error": {
        "code": "STALE_UPDATE",
        "message": "Server has newer version",
        "server_state": {
          "id": "550e8400-...",
          "name": "محمد أحمد علي",
          "updated_at": "2026-09-08T11:30:00Z"
        }
      }
    }
  ]
}
```

**Android:** يعتمد `server_state` كنسخة جديدة (latest-write-wins).

---

## 15. Pydantic Schemas (Backend)

```python
# app/schemas/auth.py
from pydantic import BaseModel, Field


class LoginRequest(BaseModel):
    username: str = Field(min_length=3, max_length=50)
    password: str = Field(min_length=8, max_length=128)


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    expires_in: int
    user: "UserResponse"


class RefreshRequest(BaseModel):
    refresh_token: str


class UserResponse(BaseModel):
    id: str
    username: str
    role: str
    is_active: bool
    last_login_at: str | None = None
```

```python
# app/schemas/customer.py
from pydantic import BaseModel, Field, field_validator
import re


class CustomerBase(BaseModel):
    name: str = Field(min_length=2, max_length=120)
    phone: str = Field(min_length=7, max_length=30)
    latitude: float = Field(ge=-90, le=90)
    longitude: float = Field(ge=-180, le=180)
    accuracy: float | None = Field(default=None, ge=0)

    @field_validator("name")
    @classmethod
    def normalize_name(cls, v: str) -> str:
        return v.strip()

    @field_validator("phone")
    @classmethod
    def normalize_phone(cls, v: str) -> str:
        v = re.sub(r"[\s\-()]", "", v.strip())
        return v


class CustomerCreate(CustomerBase):
    pass


class CustomerUpdate(BaseModel):
    name: str | None = Field(default=None, min_length=2, max_length=120)
    phone: str | None = Field(default=None, min_length=7, max_length=30)
    latitude: float | None = Field(default=None, ge=-90, le=90)
    longitude: float | None = Field(default=None, ge=-180, le=180)
    accuracy: float | None = Field(default=None, ge=0)


class CustomerResponse(CustomerBase):
    id: str
    created_at: str
    updated_at: str
```

```python
# app/schemas/delivery.py
from pydantic import BaseModel, Field
from enum import Enum


class DeliveryStatus(str, Enum):
    PENDING = "PENDING"
    ASSIGNED = "ASSIGNED"
    ON_THE_WAY = "ON_THE_WAY"
    ARRIVED = "ARRIVED"
    DELIVERED = "DELIVERED"
    CANCELLED = "CANCELLED"


class DeliveryCreate(BaseModel):
    customer_id: str
    status: DeliveryStatus = DeliveryStatus.ON_THE_WAY


class DeliveryStatusUpdate(BaseModel):
    status: DeliveryStatus


class DeliveryResponse(BaseModel):
    id: str
    customer_id: str
    customer: "CustomerResponse | None" = None
    status: DeliveryStatus
    created_at: str
    started_at: str | None = None
    arrived_at: str | None = None
    completed_at: str | None = None
    cancelled_at: str | None = None
```

```python
# app/schemas/sync.py
from pydantic import BaseModel
from typing import Any


class SyncOperationRequest(BaseModel):
    id: str
    entity_type: str       # CUSTOMER | DELIVERY
    operation: str         # CREATE | UPDATE | DELETE | COMPLETE
    payload: dict[str, Any]
    idempotency_key: str | None = None


class SyncRequest(BaseModel):
    operations: list[SyncOperationRequest]
    latest_sync_timestamp: str | None = None


class SyncOperationResult(BaseModel):
    operation_id: str
    status: str             # SUCCESS | CONFLICT | FAILED | IGNORED
    entity_id: str | None = None
    server_state: dict[str, Any] | None = None
    error: dict[str, Any] | None = None


class ServerChange(BaseModel):
    entity_type: str
    entity_id: str
    operation: str
    payload: dict[str, Any]


class SyncResponse(BaseModel):
    results: list[SyncOperationResult]
    server_changes: list[ServerChange] = []
    latest_sync_timestamp: str
```

---

## 16. Retrofit API Interfaces (Android)

```kotlin
// data/remote/api/AuthApi.kt
interface AuthApi {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<TokenResponse>

    @POST("auth/refresh")
    suspend fun refresh(
        @Body request: RefreshRequest
    ): Response<TokenResponse>

    @POST("auth/logout")
    suspend fun logout(
        @Body request: RefreshRequest
    ): Response<Unit>

    @GET("auth/me")
    suspend fun getMe(): Response<UserResponse>
}
```

```kotlin
// data/remote/api/CustomerApi.kt
interface CustomerApi {

    @GET("customers")
    suspend fun listCustomers(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
        @Query("search") search: String? = null
    ): Response<PaginatedResponse<CustomerResponse>>

    @POST("customers")
    suspend fun createCustomer(
        @Body request: CustomerCreate
    ): Response<CustomerResponse>

    @GET("customers/{id}")
    suspend fun getCustomer(
        @Path("id") id: String
    ): Response<CustomerResponse>

    @PATCH("customers/{id}")
    suspend fun updateCustomer(
        @Path("id") id: String,
        @Body request: CustomerUpdate
    ): Response<CustomerResponse>

    @DELETE("customers/{id}")
    suspend fun deleteCustomer(
        @Path("id") id: String
    ): Response<Unit>
}
```

```kotlin
// data/remote/api/DeliveryApi.kt
interface DeliveryApi {

    @GET("deliveries")
    suspend fun listDeliveries(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
        @Query("status") status: String? = null,
        @Query("from_date") fromDate: String? = null,
        @Query("to_date") toDate: String? = null
    ): Response<PaginatedResponse<DeliveryResponse>>

    @POST("deliveries")
    suspend fun createDelivery(
        @Body request: DeliveryCreate
    ): Response<DeliveryResponse>

    @GET("deliveries/{id}")
    suspend fun getDelivery(
        @Path("id") id: String
    ): Response<DeliveryResponse>

    @PATCH("deliveries/{id}/status")
    suspend fun updateStatus(
        @Path("id") id: String,
        @Body request: DeliveryStatusUpdate,
        @Header("Idempotency-Key") idempotencyKey: String
    ): Response<DeliveryResponse>
}
```

```kotlin
// data/remote/api/SyncApi.kt
interface SyncApi {

    @POST("sync")
    suspend fun sync(
        @Body request: SyncRequest,
        @Header("Idempotency-Key") idempotencyKey: String? = null
    ): Response<SyncResponse>
}
```

---

## 17. OkHttp Interceptors (Android)

```kotlin
// data/remote/interceptor/AuthInterceptor.kt
class AuthInterceptor(
    private val tokenProvider: suspend () -> String?
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenProvider() }
        val request = chain.request().newBuilder()
            .apply {
                if (token != null && chain.request().header("Authorization") == null) {
                    header("Authorization", "Bearer $token")
                }
            }
            .build()
        return chain.proceed(request)
    }
}
```

```kotlin
// data/remote/interceptor/TokenAuthenticator.kt
class TokenAuthenticator(
    private val refreshTokenUseCase: suspend () -> String?,
    private val onLogout: () -> Unit
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.code != 401) return null

        val newToken = runBlocking {
            refreshTokenUseCase() ?: return@runBlocking null
        } ?: run {
            onLogout()
            return null
        }

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }
}
```

---

**نهاية وثيقة API Contract — WASLNI v1**
