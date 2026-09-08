# WASLNI — Performance Targets & Optimization

> Performance benchmarks and optimization strategies for the WASLNI app.

## Android Performance Targets

| Metric | Target | How to Measure |
|--------|--------|----------------|
| App startup (cold) | < 2 seconds | `adb shell am start -W com.waslni.driver/.presentation.MainActivity` |
| Map load | < 1.5 seconds | Manual — from Home screen open to first frame |
| Search (local, 5000 customers) | < 100ms | `./gradlew testDebugUnitTest --tests "*CustomerDaoTest*"` |
| Customer list render | < 16ms/frame | Android Studio Profiler → CPU |
| Memory usage | < 200 MB | Android Studio Profiler → Memory |
| Battery (1hr active use) | < 15% | On-device measurement (Phase 24) |
| API response (95th percentile) | < 500ms | Backend logs + httpx timing |

## Android Optimizations Applied

### 1. Marker Clustering (Phase 23)
```kotlin
// When ≥50 markers → grid-based clustering
// Cell size: 0.005° (~500m) → markers in the same cell merge into one cluster
// O(n) algorithm — fast for thousands of markers
val clusters = clusterMarkers(markers, cellSizeDegrees = 0.005, threshold = 50)
```

- Below 50 markers: render individually (no overhead).
- 50+ markers: grid-based clustering → each cell becomes one cluster marker with a count badge.
- Cell size scales with zoom level (smaller cells at higher zoom).

### 2. OkHttp Response Cache (Phase 23)
```kotlin
val cache = Cache(context.cacheDir.resolve("http_cache"), 10 * 1024 * 1024) // 10 MB
OkHttpClient.Builder().cache(cache)...
```
- Caches GET responses when the server sends `Cache-Control: public, max-age=N`.
- Authenticated responses are NOT cached (user-specific data + token leakage risk).
- Saves bandwidth + speeds up repeated requests (e.g. /auth/me on startup).

### 3. LazyColumn with Keys
```kotlin
LazyColumn {
    items(customers, key = { it.id }) { customer ->  // stable key → proper diffing
        CustomerRow(customer)
    }
}
```
- Only renders visible items (not the whole list).
- Stable keys enable efficient item reordering + animations.
- No performance degradation with 5000+ customers.

### 4. Flow + StateFlow (Reactive)
```kotlin
val state: StateFlow<HomeUiState> = combine(
    observeCustomers(),       // Flow from Room — only emits on change
    observeActiveIds(),
    ...
).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue)
```
- Room emits only when data changes — no polling.
- WhileSubscribed(5000) → stops collecting 5s after the last subscriber leaves → saves battery.
- Single combine → one emission per state change (no intermediate renders).

### 5. Battery-Aware Location Updates
| State | Update Interval | Rationale |
|-------|----------------|----------|
| Home screen (idle) | 15 seconds | Low frequency — just keeping the driver marker fresh |
| Active delivery (ON_THE_WAY) | 5 seconds | Higher frequency for arrival detection |
| Navigation active | 3 seconds | Highest frequency for turn-by-turn guidance |
| Screen off | Stopped | No tracking when the app is backgrounded (unless navigating) |

### 6. Skeleton Loading (Phase 20)
- SkeletonList shows shimmering placeholders rows while data loads from Room.
- No layout jump when real data arrives — the skeleton mimics the actual row layout.
- Better perceived performance than a blank screen + spinner.

## Backend Performance Targets

| Metric | Target | How to Measure |
|--------|--------|----------------|
| API response (p50) | < 200ms | `pytest --durations=10` + production logs |
| API response (p95) | < 500ms | Same |
| API response (p99) | < 1s | Same |
| DB query (single row by PK) | < 5ms | EXPLAIN ANALYZE |
| DB query (paginated list) | < 30ms | EXPLAIN ANALYZE |
| DB query (search ILIKE) | < 50ms | EXPLAIN ANALYZE |
| Sync endpoint (50 ops) | < 2s | `pytest tests/test_sync.py` |

## Backend Optimizations Applied

### 1. Database Indexes (Phase 7)
```sql
-- Customers: composite indexes for common query patterns
CREATE INDEX idx_customers_driver_name ON customers(driver_id, name);
CREATE INDEX idx_customers_driver_phone ON customers(driver_id, phone);
CREATE UNIQUE INDEX uq_driver_phone ON customers(driver_id, phone);

-- Deliveries: indexes for status + date filtering
CREATE INDEX idx_deliveries_driver_status ON deliveries(driver_id, status);
CREATE INDEX idx_deliveries_driver_created ON deliveries(driver_id, created_at DESC);
CREATE INDEX idx_deliveries_customer ON deliveries(customer_id);

-- Refresh tokens: lookup by hash + user
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);

-- Audit logs: by user + date, by action
CREATE INDEX idx_audit_user_date ON audit_logs(user_id, created_at DESC);
CREATE INDEX idx_audit_action ON audit_logs(action);

-- Idempotency: lookup by key + user, cleanup by expiry
CREATE INDEX idx_idempotency_key_user ON idempotency_keys(key, user_id);
CREATE INDEX idx_idempotency_expires ON idempotency_keys(expires_at);
```

### 2. Eager Loading (N+1 Fix)
```python
# DeliveryRepository.get_by_id — eager-loads customer to avoid N+1
async def get_by_id(self, delivery_id, driver_id):
    result = await self.session.execute(
        select(Delivery)
        .options(selectinload(Delivery.customer))  # ← eager load
        .where(Delivery.id == delivery_id, Delivery.driver_id == driver_id)
    )
    return result.scalar_one_or_none()
```
Without this, each delivery response would trigger a second query for the customer → N+1 problem.

### 3. Pagination (Not Fetch-All)
```python
# CustomerRepository.list_customers — paginated, not fetch-all
data_stmt = (
    select(Customer)
    .where(*conditions)
    .order_by(Customer.name.asc())
    .offset(offset)  # ← pagination
    .limit(limit)
)
```
- Default page size: 50, max: 200.
- Offset + LIMIT → the DB only scans the needed rows.

### 4. Count Optimization
```python
# Separate COUNT query — not len(fetch-all)
count_stmt = select(func.count()).select_from(Customer).where(*conditions)
total = (await self.session.execute(count_stmt)).scalar_one()
```
COUNT(*) is optimized by PostgreSQL (uses index-only scan on the PK).

### 5. Connection Pool
```python
engine = create_async_engine(
    DATABASE_URL,
    pool_pre_ping=True,   # check connection before checkout (handles DB restarts)
    pool_size=10,          # 10 persistent connections
    max_overflow=20,       # +20 burst connections = 30 max
    pool_timeout=30,       # wait up to 30s for a connection
)
```

### 6. Query Timer (Phase 23)
```python
async with QueryTimer("get_customer_by_id"):
    customer = await session.execute(...)
# Logs warning if query > 100ms
```

### 7. Rate Limiting (Phase 21)
- Prevents a single client from overwhelming the server.
- In-memory sliding window (production: Redis).

## Performance Testing

### Android
```bash
# CPU profiler
./gradlew assembleDebug
# Open Android Studio → Profile → CPU/Memory

# Benchmark with Macrobenchmark (future)
./gradlew :benchmark:connectedBenchmarkAndroidTest
```

### Backend
```bash
# Test durations (slowest tests)
pytest --durations=10

# EXPLAIN ANALYZE on a specific query
python -c "
import asyncio
from app.core.database import AsyncSessionLocal
from app.core.query_optimization import explain_query

async def main():
    async with AsyncSessionLocal() as s:
        plan = await explain_query(s, 'SELECT * FROM customers WHERE driver_id = :did LIMIT 50', {'did': '...'})
        print(plan)

asyncio.run(main())
"

# Load testing (future — Phase 28)
# locust -f tests/load_test.py --host=http://localhost:8000
```

## Performance Checklist (Pre-Launch)

- [x] Marker clustering for 50+ markers
- [x] OkHttp response cache (10 MB)
- [x] LazyColumn with stable keys
- [x] Flow + WhileSubscribed(5000) for battery
- [x] Battery-aware location update intervals
- [x] Skeleton loading placeholders
- [x] Database indexes on all query columns
- [x] Eager loading (no N+1 queries)
- [x] Pagination on list endpoints
- [x] Connection pool (10 + 20 overflow)
- [x] Query timer for slow query detection
- [x] Rate limiting (100/min global, 5/min login)
- [ ] Memory profiling on real device (Phase 24)
- [ ] Battery measurement on real device (Phase 24)
- [ ] Load testing (Phase 28)
