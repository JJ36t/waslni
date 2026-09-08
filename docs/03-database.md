# وصلني (WASLNI) — Database Schema Design

> **النسخة:** 1.0
> **التاريخ:** 2026-09-08
> **المرحلة:** Planning

---

## 1. نظرة عامة (Overview)

لدينا طبقتا بيانات:

| الطبقة | التقنية | الدور |
|-------|--------|------|
| **Backend DB** | PostgreSQL | Source of Truth المركزي |
| **Local DB** | Room (SQLite) | Source of Read للـ UI، Offline First |

كل جدول له نسختان متطابقتان تقريبًا، لكن مع اختلافات بسيطة:
- PostgreSQL يستخدم `UUID`, `TIMESTAMP`, `Numeric(10,7)`
- Room يستخدم `String` (للـ UUID), `Long` (epoch millis), `Double`

---

## 2. PostgreSQL Schema (Backend — Source of Truth)

### 2.1 ER Diagram

```
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│    users     │ 1     N │  customers   │ 1     N │  deliveries  │
│──────────────│─────────│──────────────│─────────│──────────────│
│ id (UUID) PK │         │ id (UUID) PK │         │ id (UUID) PK │
│ username     │         │ driver_id FK │         │ customer_id  │
│ password_hash│         │ name         │         │ driver_id    │
│ role         │         │ phone        │         │ status       │
│ is_active    │         │ latitude     │         │ created_at   │
│ created_at   │         │ longitude    │         │ started_at   │
│ updated_at   │         │ accuracy     │         │ arrived_at   │
│ last_login   │         │ created_at   │         │ completed_at │
└──────────────┘         │ updated_at   │         │ cancelled_at │
        │ 1               └──────────────┘         └──────────────┘
        │                                                    │
        │                ┌──────────────────┐                │
        └───────────────>│  refresh_tokens  │<───────────────┘
                         │──────────────────│     (created_by)
                         │ id (UUID) PK     │
                         │ user_id FK       │
                         │ token_hash       │
                         │ expires_at       │
                         │ revoked          │
                         │ created_at       │
                         └──────────────────┘

┌──────────────────┐         ┌──────────────────┐
│   audit_logs     │         │  idempotency     │
│──────────────────│         │──────────────────│
│ id (UUID) PK     │         │ id (UUID) PK     │
│ user_id FK       │         │ key (unique)     │
│ action           │         │ user_id          │
│ entity_type      │         │ endpoint         │
│ entity_id        │         │ request_hash     │
│ metadata (JSONB) │         │ response (JSONB) │
│ ip_address       │         │ created_at       │
│ created_at       │         │ expires_at       │
└──────────────────┘         └──────────────────┘
```

### 2.2 جدول `users`

| الحقل | النوع | القيود | الوصف |
|------|------|-------|------|
| `id` | UUID | PK, default `gen_random_uuid()` | معرّف المستخدم |
| `username` | VARCHAR(50) | UNIQUE, NOT NULL | اسم المستخدم للدخول |
| `password_hash` | VARCHAR(255) | NOT NULL | Argon2id hash |
| `role` | VARCHAR(20) | NOT NULL, default `'driver'` | `driver` / `admin` |
| `is_active` | BOOLEAN | NOT NULL, default `true` | الحساب مفعّل؟ |
| `created_at` | TIMESTAMP | NOT NULL, default `now()` | تاريخ الإنشاء |
| `updated_at` | TIMESTAMP | NOT NULL, default `now()` | تاريخ آخر تحديث |
| `last_login_at` | TIMESTAMP | nullable | آخر تسجيل دخول |

**Indexes:**
- UNIQUE index على `username`.

```sql
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(50) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'driver' CHECK (role IN ('driver', 'admin')),
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    last_login_at   TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
```

### 2.3 جدول `customers`

| الحقل | النوع | القيود | الوصف |
|------|------|-------|------|
| `id` | UUID | PK | معرّف الزبون |
| `driver_id` | UUID | FK → users(id), NOT NULL | مندوب الزبون |
| `name` | VARCHAR(120) | NOT NULL, length 2-120 | اسم الزبون |
| `phone` | VARCHAR(30) | NOT NULL, length 7-30 | رقم الموبايل |
| `latitude` | NUMERIC(10,7) | NOT NULL, CHECK (-90, 90) | خط العرض |
| `longitude` | NUMERIC(10,7) | NOT NULL, CHECK (-180, 180) | خط الطول |
| `accuracy` | FLOAT | nullable, CHECK ≥ 0 | دقة GPS بالأمتار |
| `created_at` | TIMESTAMP | NOT NULL | تاريخ الإنشاء |
| `updated_at` | TIMESTAMP | NOT NULL | تاريخ آخر تحديث |

**Indexes:**
- Composite UNIQUE: `(driver_id, phone)` — لا تكرار للرقم عند نفس المندوب.
- Index على `(driver_id, name)` للبحث.
- GiST index على إحداثيات للمسافة (لاحقًا).

```sql
CREATE TABLE customers (
    id          UUID PRIMARY KEY,
    driver_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(120) NOT NULL CHECK (char_length(name) >= 2),
    phone       VARCHAR(30) NOT NULL CHECK (char_length(phone) >= 7),
    latitude    NUMERIC(10, 7) NOT NULL CHECK (latitude >= -90 AND latitude <= 90),
    longitude   NUMERIC(10, 7) NOT NULL CHECK (longitude >= -180 AND longitude <= 180),
    accuracy    FLOAT CHECK (accuracy IS NULL OR accuracy >= 0),
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL,
    CONSTRAINT uq_driver_phone UNIQUE (driver_id, phone)
);

CREATE INDEX idx_customers_driver_name ON customers(driver_id, name);
CREATE INDEX idx_customers_driver_phone ON customers(driver_id, phone);
```

**لماذا NUMERIC(10,7)?**
- 3 أرقام قبل الفاصلة (يكفي لخطوط العرض -90 إلى 90 وخطوط الطول -180 إلى 180).
- 7 أرقام بعد الفاصلة = دقة 1.1cm.
- أكثر من دقة GPS الفعلية (~3-5m) بكثير، لكن نتجنب أي فقدان للدقة.

### 2.4 جدول `deliveries`

| الحقل | النوع | القيود | الوصف |
|------|------|-------|------|
| `id` | UUID | PK | معرّف التوصيل |
| `customer_id` | UUID | FK → customers(id), NOT NULL | الزبون |
| `driver_id` | UUID | FK → users(id), NOT NULL | المندوب |
| `status` | VARCHAR(20) | NOT NULL, CHECK | حالة التوصيل |
| `created_at` | TIMESTAMP | NOT NULL | تاريخ الإنشاء |
| `started_at` | TIMESTAMP | nullable | بدء التوصيل |
| `arrived_at` | TIMESTAMP | nullable | الوصول |
| `completed_at` | TIMESTAMP | nullable | إكمال التوصيل |
| `cancelled_at` | TIMESTAMP | nullable | الإلغاء |

**States:**
`PENDING`, `ASSIGNED`, `ON_THE_WAY`, `ARRIVED`, `DELIVERED`, `CANCELLED`

```sql
CREATE TABLE deliveries (
    id            UUID PRIMARY KEY,
    customer_id   UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    driver_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status        VARCHAR(20) NOT NULL CHECK (
                      status IN ('PENDING', 'ASSIGNED', 'ON_THE_WAY',
                                 'ARRIVED', 'DELIVERED', 'CANCELLED')
                  ),
    created_at    TIMESTAMP NOT NULL,
    started_at    TIMESTAMP,
    arrived_at    TIMESTAMP,
    completed_at  TIMESTAMP,
    cancelled_at  TIMESTAMP,
    CONSTRAINT chk_delivery_times CHECK (
        (status = 'PENDING' AND started_at IS NULL AND completed_at IS NULL) OR
        (status IN ('ASSIGNED', 'ON_THE_WAY') AND started_at IS NOT NULL AND completed_at IS NULL) OR
        (status = 'ARRIVED' AND started_at IS NOT NULL AND arrived_at IS NOT NULL AND completed_at IS NULL) OR
        (status = 'DELIVERED' AND started_at IS NOT NULL AND completed_at IS NOT NULL) OR
        (status = 'CANCELLED' AND cancelled_at IS NOT NULL)
    )
);

CREATE INDEX idx_deliveries_driver_status ON deliveries(driver_id, status);
CREATE INDEX idx_deliveries_driver_created ON deliveries(driver_id, created_at DESC);
CREATE INDEX idx_deliveries_customer ON deliveries(customer_id);
```

### 2.5 جدول `refresh_tokens`

يسمح بـ revocation + multi-device.

| الحقل | النوع | الوصف |
|------|------|------|
| `id` | UUID | PK |
| `user_id` | UUID | FK → users |
| `token_hash` | VARCHAR(255) | Hash of refresh token |
| `expires_at` | TIMESTAMP | انتهاء الصلاحية |
| `revoked` | BOOLEAN | مفعّل؟ |
| `created_at` | TIMESTAMP | تاريخ الإنشاء |
| `device_info` | VARCHAR(255) | معلومات الجهاز (nullable) |

```sql
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    device_info VARCHAR(255)
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);
```

### 2.6 جدول `audit_logs`

```sql
CREATE TABLE audit_logs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID REFERENCES users(id) ON DELETE SET NULL,
    action       VARCHAR(50) NOT NULL,
    entity_type  VARCHAR(30),
    entity_id    UUID,
    metadata     JSONB,
    ip_address   VARCHAR(45),
    created_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_user_date ON audit_logs(user_id, created_at DESC);
CREATE INDEX idx_audit_action ON audit_logs(action);
```

**Actions الموثقة:**
`LOGIN_SUCCESS`, `LOGIN_FAILURE`, `LOGOUT`, `CREATE_CUSTOMER`, `UPDATE_CUSTOMER`, `DELETE_CUSTOMER`, `START_DELIVERY`, `COMPLETE_DELIVERY`, `CANCEL_DELIVERY`, `TOKEN_REFRESHED`, `TOKEN_REVOKED`.

### 2.7 جدول `idempotency_keys`

```sql
CREATE TABLE idempotency_keys (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key           VARCHAR(255) NOT NULL UNIQUE,
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    endpoint      VARCHAR(100) NOT NULL,
    request_hash  VARCHAR(64) NOT NULL,
    response      JSONB NOT NULL,
    status_code   INTEGER NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    expires_at    TIMESTAMP NOT NULL DEFAULT (now() + INTERVAL '24 hours')
);

CREATE INDEX idx_idempotency_key_user ON idempotency_keys(key, user_id);
CREATE INDEX idx_idempotency_expires ON idempotency_keys(expires_at);
```

### 2.8 جداول لاحقة (Post-MVP)

```sql
-- routes (لاحقًا - Multi-stop)
CREATE TABLE routes (
    id          UUID PRIMARY KEY,
    driver_id   UUID NOT NULL REFERENCES users(id),
    status      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

CREATE TABLE route_stops (
    id              UUID PRIMARY KEY,
    route_id        UUID NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    customer_id     UUID NOT NULL REFERENCES customers(id),
    stop_order      INTEGER NOT NULL,
    status          VARCHAR(20) NOT NULL
);

-- notifications (لاحقًا)
CREATE TABLE notifications (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id),
    title       VARCHAR(255),
    body        TEXT,
    type        VARCHAR(50),
    is_read     BOOLEAN DEFAULT false,
    created_at  TIMESTAMP NOT NULL
);

-- delivery_zones (لاحقًا)
CREATE TABLE delivery_zones (
    id          UUID PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    boundary    JSONB NOT NULL  -- GeoJSON Polygon
);
```

---

## 3. Room Schema (Android — Local DB)

### 3.1 ER Diagram

```
┌──────────────────┐         ┌──────────────────┐
│ customer_entity  │ 1     N │ delivery_entity  │
│──────────────────│─────────│──────────────────│
│ id (String) PK   │         │ id (String) PK   │
│ name             │         │ customerId       │
│ phone            │         │ status           │
│ latitude         │         │ createdAt        │
│ longitude        │         │ startedAt        │
│ accuracy         │         │ arrivedAt        │
│ createdAt        │         │ completedAt      │
│ updatedAt        │         │ cancelledAt      │
│ syncState        │         │ syncState        │
└──────────────────┘         └──────────────────┘
        │
        │
        ▼
┌──────────────────────┐
│ sync_operation_entity│
│──────────────────────│
│ id (String) PK       │
│ entityId (String)    │
│ entityType           │
│ operation            │
│ payload (String)     │
│ createdAt (Long)     │
│ retryCount (Int)     │
│ status               │
└──────────────────────┘

┌──────────────────┐
│ session_entity    │  (optional - cached)
│──────────────────│
│ key (String) PK   │
│ value (String)    │
└──────────────────┘
```

### 3.2 `CustomerEntity`

```kotlin
@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["phone"]),
        Index(value = ["name"]),
        Index(value = ["syncState"])
    ]
)
data class CustomerEntity(
    @PrimaryKey
    val id: String,

    val name: String,

    val phone: String,

    val latitude: Double,

    val longitude: Double,

    val accuracy: Float?,

    val createdAt: Long,

    val updatedAt: Long,

    /**
     * SYNCED: تمت مزامنته مع السيرفر
     * PENDING: بانتظار المزامنة
     * FAILED: فشلت المزامنة بعد عدة محاولات
     */
    val syncState: String = "PENDING"
)
```

### 3.3 `DeliveryEntity`

```kotlin
@Entity(
    tableName = "deliveries",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["status"]),
        Index(value = ["createdAt"]),
        Index(value = ["syncState"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class DeliveryEntity(
    @PrimaryKey
    val id: String,

    val customerId: String,

    val status: String,

    val createdAt: Long,

    val startedAt: Long?,

    val arrivedAt: Long?,

    val completedAt: Long?,

    val cancelledAt: Long?,

    val syncState: String = "PENDING"
)
```

### 3.4 `SyncOperationEntity`

```kotlin
@Entity(
    tableName = "sync_operations",
    indices = [
        Index(value = ["status"]),
        Index(value = ["entityId"]),
        Index(value = ["createdAt"])
    ]
)
data class SyncOperationEntity(
    @PrimaryKey
    val id: String,

    val entityId: String,

    val entityType: String,   // CUSTOMER, DELIVERY

    val operation: String,    // CREATE, UPDATE, DELETE, COMPLETE

    val payload: String,      // JSON

    val createdAt: Long,

    val retryCount: Int = 0,

    val status: String        // PENDING, SYNCING, SYNCED, FAILED
)
```

### 3.5 WaselDatabase

```kotlin
@Database(
    entities = [
        CustomerEntity::class,
        DeliveryEntity::class,
        SyncOperationEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(WaselConverters::class)
abstract class WaselDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun deliveryDao(): DeliveryDao
    abstract fun syncDao(): SyncOperationDao
}
```

---

## 4. DAOs

### 4.1 CustomerDao

```kotlin
@Dao
interface CustomerDao {

    @Query("""
        SELECT * FROM customers
        WHERE name LIKE '%' || :query || '%'
           OR phone LIKE '%' || :query || '%'
        ORDER BY name COLLATE NOCASE ASC
    """)
    fun search(query: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE id = :id")
    fun observeById(id: String): Flow<CustomerEntity?>

    @Query("SELECT * FROM customers WHERE phone = :phone LIMIT 1")
    suspend fun getByPhone(phone: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: CustomerEntity)

    @Update
    suspend fun update(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM customers WHERE syncState = :state")
    suspend fun getBySyncState(state: String): List<CustomerEntity>

    @Query("UPDATE customers SET syncState = :state WHERE id = :id")
    suspend fun updateSyncState(id: String, state: String)

    @Query("SELECT COUNT(*) FROM customers")
    fun observeCount(): Flow<Int>
}
```

### 4.2 DeliveryDao

```kotlin
@Dao
interface DeliveryDao {

    @Query("SELECT * FROM deliveries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DeliveryEntity>>

    @Query("""
        SELECT * FROM deliveries
        WHERE createdAt >= :startOfDay AND createdAt < :endOfDay
        ORDER BY createdAt DESC
    """)
    fun observeByDateRange(startOfDay: Long, endOfDay: Long): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE id = :id")
    suspend fun getById(id: String): DeliveryEntity?

    @Query("SELECT * FROM deliveries WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun observeByCustomer(customerId: String): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE status = :status ORDER BY createdAt DESC")
    fun observeByStatus(status: String): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE customerId = :customerId AND status IN ('ON_THE_WAY', 'ARRIVED') LIMIT 1")
    suspend fun getActiveForCustomer(customerId: String): DeliveryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(delivery: DeliveryEntity)

    @Update
    suspend fun update(delivery: DeliveryEntity)

    @Query("UPDATE deliveries SET status = :status, completedAt = :completedAt, syncState = :syncState WHERE id = :id")
    suspend fun markCompleted(id: String, status: String, completedAt: Long, syncState: String)

    @Query("UPDATE deliveries SET syncState = :state WHERE id = :id")
    suspend fun updateSyncState(id: String, state: String)

    @Query("SELECT COUNT(*) FROM deliveries WHERE status = :status AND createdAt >= :startOfDay")
    fun observeTodayCountByStatus(status: String, startOfDay: Long): Flow<Int>
}
```

### 4.3 SyncOperationDao

```kotlin
@Dao
interface SyncOperationDao {

    @Query("SELECT * FROM sync_operations WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPending(): List<SyncOperationEntity>

    @Query("SELECT COUNT(*) FROM sync_operations WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(op: SyncOperationEntity)

    @Query("UPDATE sync_operations SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE sync_operations SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: String)

    @Query("UPDATE sync_operations SET status = 'FAILED' WHERE id = :id")
    suspend fun markFailed(id: String)

    @Query("DELETE FROM sync_operations WHERE status = 'SYNCED' AND createdAt < :before")
    suspend fun cleanOldSynced(before: Long)

    @Query("SELECT * FROM sync_operations WHERE entityId = :entityId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestForEntity(entityId: String): SyncOperationEntity?
}
```

---

## 5. Domain Models (Pure Kotlin)

```kotlin
// domain/model/Customer.kt
data class Customer(
    val id: String,
    val name: String,
    val phone: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val createdAt: Long,
    val updatedAt: Long
)

// domain/model/Delivery.kt
data class Delivery(
    val id: String,
    val customerId: String,
    val status: DeliveryStatus,
    val createdAt: Long,
    val startedAt: Long?,
    val arrivedAt: Long?,
    val completedAt: Long?,
    val cancelledAt: Long?
)

enum class DeliveryStatus {
    PENDING, ASSIGNED, ON_THE_WAY, ARRIVED, DELIVERED, CANCELLED;

    companion object {
        fun fromString(value: String): DeliveryStatus =
            valueOf(value)
    }
}

// domain/model/LocationResult.kt
data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long
)

// domain/model/RouteResult.kt
data class RouteResult(
    val distanceMeters: Double,
    val durationSeconds: Long,
    val geometry: List<LatLng>,
    val instructions: List<RouteInstruction>
)

data class LatLng(val latitude: Double, val longitude: Double)

data class RouteInstruction(
    val instruction: String,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val maneuverType: String  // turn, arrive, depart, etc.
)
```

---

## 6. Mappers (Entity ↔ Domain ↔ DTO)

```kotlin
// data/local/mapper/CustomerMapper.kt
fun CustomerEntity.toDomain(): Customer = Customer(
    id = id,
    name = name,
    phone = phone,
    latitude = latitude,
    longitude = longitude,
    accuracy = accuracy,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Customer.toEntity(syncState: String = "PENDING"): CustomerEntity = CustomerEntity(
    id = id,
    name = name,
    phone = phone,
    latitude = latitude,
    longitude = longitude,
    accuracy = accuracy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncState = syncState
)
```

---

## 7. SQLAlchemy Models (Backend)

```python
# app/models/user.py
import uuid
from datetime import datetime
from sqlalchemy import Column, String, Boolean, DateTime, CheckConstraint
from sqlalchemy.dialects.postgresql import UUID
from app.core.database import Base


class User(Base):
    __tablename__ = "users"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    username = Column(String(50), unique=True, nullable=False, index=True)
    password_hash = Column(String(255), nullable=False)
    role = Column(String(20), nullable=False, default="driver")
    is_active = Column(Boolean, nullable=False, default=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)
    last_login_at = Column(DateTime, nullable=True)

    __table_args__ = (
        CheckConstraint("role IN ('driver', 'admin')", name="chk_user_role"),
    )
```

```python
# app/models/customer.py
from sqlalchemy import Column, String, Float, DateTime, ForeignKey, CheckConstraint, UniqueConstraint
from sqlalchemy.dialects.postgresql import UUID, NUMERIC
from app.core.database import Base


class Customer(Base):
    __tablename__ = "customers"

    id = Column(UUID(as_uuid=True), primary_key=True)
    driver_id = Column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    name = Column(String(120), nullable=False)
    phone = Column(String(30), nullable=False)
    latitude = Column(NUMERIC(10, 7), nullable=False)
    longitude = Column(NUMERIC(10, 7), nullable=False)
    accuracy = Column(Float, nullable=True)
    created_at = Column(DateTime, nullable=False)
    updated_at = Column(DateTime, nullable=False)

    __table_args__ = (
        UniqueConstraint("driver_id", "phone", name="uq_driver_phone"),
        CheckConstraint("char_length(name) >= 2", name="chk_customer_name_length"),
        CheckConstraint("char_length(phone) >= 7", name="chk_customer_phone_length"),
        CheckConstraint("latitude >= -90 AND latitude <= 90", name="chk_latitude"),
        CheckConstraint("longitude >= -180 AND longitude <= 180", name="chk_longitude"),
    )
```

```python
# app/models/delivery.py
from sqlalchemy import Column, String, DateTime, ForeignKey, CheckConstraint
from sqlalchemy.dialects.postgresql import UUID
from app.core.database import Base


class Delivery(Base):
    __tablename__ = "deliveries"

    id = Column(UUID(as_uuid=True), primary_key=True)
    customer_id = Column(UUID(as_uuid=True), ForeignKey("customers.id", ondelete="RESTRICT"), nullable=False)
    driver_id = Column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    status = Column(String(20), nullable=False)
    created_at = Column(DateTime, nullable=False)
    started_at = Column(DateTime, nullable=True)
    arrived_at = Column(DateTime, nullable=True)
    completed_at = Column(DateTime, nullable=True)
    cancelled_at = Column(DateTime, nullable=True)

    __table_args__ = (
        CheckConstraint(
            "status IN ('PENDING', 'ASSIGNED', 'ON_THE_WAY', 'ARRIVED', 'DELIVERED', 'CANCELLED')",
            name="chk_delivery_status"
        ),
    )
```

---

## 8. Alembic Migrations Plan

| # | اسم الملف | المحتوى |
|---|---------|--------|
| 001 | `001_create_users.py` | جدول `users` |
| 002 | `002_create_customers.py` | جدول `customers` + indexes |
| 003 | `003_create_deliveries.py` | جدول `deliveries` + indexes |
| 004 | `004_create_refresh_tokens.py` | جدول `refresh_tokens` |
| 005 | `005_create_audit_logs.py` | جدول `audit_logs` |
| 006 | `006_create_idempotency_keys.py` | جدول `idempotency_keys` |

---

## 9. Query Patterns & Performance

### 9.1 Backend Queries

```python
# Get customers for a driver (paginated)
SELECT * FROM customers
WHERE driver_id = :driver_id
  AND (:search IS NULL OR name ILIKE :search || '%' OR phone ILIKE :search || '%')
ORDER BY name
LIMIT :limit OFFSET :offset;

# Get today's deliveries for a driver
SELECT d.*, c.name as customer_name, c.phone, c.latitude, c.longitude
FROM deliveries d
JOIN customers c ON d.customer_id = c.id
WHERE d.driver_id = :driver_id
  AND d.created_at >= :start_of_day
  AND d.created_at < :end_of_day
ORDER BY d.created_at DESC;
```

### 9.2 Local Queries (Room)

```kotlin
// Search: index on (name, phone) → سريع جدًا حتى مع 5000+ سجل
// Today's deliveries: index on (createdAt)
// Active deliveries: index on (status, customerId)
// Pending sync: index on (syncState)
```

### 9.3 Expected Performance

| العملية | Backend | Local |
|--------|---------|-------|
| Search (5,000 records) | < 50ms | < 50ms |
| Today's deliveries | < 30ms | < 20ms |
| Insert customer | < 50ms | < 5ms |
| Pending sync count | < 20ms | < 5ms |

---

## 10. Data Privacy Considerations

| البيانات | الإجراء |
|---------|--------|
| Phone numbers | لا في logs، تشفير transport |
| GPS coordinates | لا في logs، حذف مع الزبون |
| Password hashes | Argon2id، لا في logs أبداً |
| JWT tokens | Hashed في DB (refresh tokens) |
| Audit metadata | لا يحوي payloads حساسة |

---

## 11. Backup Strategy

| النوع | التكرار | الاحتفاظ |
|------|--------|--------|
| Full backup | يوميًا | 7 أيام |
| Weekly backup | أسبوعيًا | 4 أسابيع |
| Monthly backup | شهريًا | 6 أشهر |

**اختبار الاستعادة:** مرة شهريًا على الأقل على بيئة Staging.

---

**نهاية وثيقة Database Schema — WASLNI v1.0**
