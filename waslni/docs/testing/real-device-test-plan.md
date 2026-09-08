# WASLNI — Real Device Test Plan

> Comprehensive test plan for validating the WASLNI app on real Android devices.
> All tests must pass before MVP launch (Phase 32).

## Test Environment

### Required Devices (minimum 2)
| Device | Android Version | Purpose |
|--------|----------------|---------|
| Primary: Pixel 6 or equivalent | Android 13+ | Main testing — modern API |
| Secondary: Samsung A-series | Android 8-10 | Compatibility — older API (minSdk 24) |
| Optional: Tablet | Android 12+ | Large screen layout |

### Test Accounts
- `test_driver` / `testpassword123` — pre-seeded via `scripts/seed.py`
- `admin` / `admin12345` — for admin operations

### Backend
- Staging server: `https://staging-api.waslni.com/api/v1/`
- Local dev server: `http://10.0.2.2:8000/api/v1/` (emulator only)

---

## Test Categories

### 1. GPS / Location Tests

#### 1.1 GPS Strong Signal (Outdoors)
| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|-----------|
| 1 | Open Add Customer screen outdoors | GPS capture starts | |
| 2 | Wait for fix | Accuracy ≤ 10m shown in green | |
| 3 | Tap "حفظ الزبون" | Customer saved with coordinates | |
| 4 | Open Home screen | Customer marker appears on map | |
| 5 | Tap marker | Bottom sheet shows name + phone + coordinates | |

#### 1.2 GPS Weak Signal (Indoors / Near Windows)
| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|-----------|
| 1 | Open Add Customer indoors | GPS capture starts | |
| 2 | Wait for fix | Accuracy > 10m shown in orange with warning | |
| 3 | Accuracy indicator shows "ضعيفة" | Warning message displayed | |
| 4 | Tap "إعادة الالتقاط" | Re-capture starts | |
| 5 | Move near window, re-capture | Accuracy improves to ≤ 10m | |

#### 1.3 GPS Disabled
| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|-----------|
| 1 | Disable GPS in system settings | | |
| 2 | Open Add Customer, tap "التقاط الموقع" | "الـGPS معطل" dialog | |
| 3 | Tap "فتح إعدادات الموقع" | System location settings open | |
| 4 | Enable GPS, return to app | Re-capture works | |

#### 1.4 GPS Permission Denied
| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|-----------|
| 1 | Revoke location permission in settings | | |
| 2 | Open Add Customer, tap "التقاط الموقع" | Rationale dialog appears | |
| 3 | Tap "سماح" | System permission dialog | |
| 4 | Deny permission | "تم رفض صلاحية الموقع" dialog | |
| 5 | Tap "فتح الإعدادات" | App details settings open | |

#### 1.5 GPS Drift (Stationary)
| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|-----------|
| 1 | Start delivery (ON_THE_WAY) | Arrival detection starts | |
| 2 | Stand still at 40m from customer | | |
| 3 | Wait 15 seconds (3 readings) | "وصلت إلى موقع الزبون" banner appears | |
| 4 | Notification "وصلت إلى موقع الزبون" received | | |
| 5 | Walk away 100m | Banner disappears, distance updates | |

#### 1.6 GPS Drift (False Arrival)
| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|-----------|
| 1 | Start delivery, stand 80m from customer | | |
| 2 | GPS spikes to 20m for 1 reading then back to 80m | | |
| 3 | Wait 15 seconds | NO arrival banner (average = 60m > 50m threshold) | |
| 4 | Walk to within 40m consistently | Arrival banner appears after 2-3 readings | |

---

### 2. Network / Offline Tests

#### 2.1 Full Online Flow
| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|-----------|
| 1 | Login with test_driver | Login succeeds, navigate to Home | |
| 2 | Add customer | Customer saved + syncs immediately | |
| 3 | Sync badge shows "متصل" (green) | No pending operations | |
| 4 | Start delivery | Delivery created + syncs | |
| 5 | Complete delivery | Delivery status = DELIVERED + syncs | |

#### 2.2 Offline Add Customer
| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|-----------|
| 1 | Turn on airplane mode | | |
| 2 | Sync badge shows "غير متصل" (red) | | |
| 3 | Add customer with GPS | Customer saved locally | |
| 4 | Sync badge shows "1 بانتظار المزامنة" (blue) | | |
| 5 | Turn off airplane mode | | |
| 6 | Wait 30 seconds | Sync worker runs, badge returns to "متصل" (green) | |
| 7 | Verify on backend (GET /customers) | Customer exists in PostgreSQL | |

#### 2.3 Offline Complete Delivery
| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|-----------|
| 1 | Turn on airplane mode | | |
| 2 | Start + complete a delivery | Status = DELIVERED locally | |
| 3 | Sync badge shows pending count | | |
| 4 | Turn off airplane mode | | |
| 5 | Wait for sync | Badge returns to green | |
| 6 | Verify delivery status on backend | DELIVERED in PostgreSQL | |

#### 2.4 Offline App Restart
| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|-----------|
| 1 | Add 3 customers offline | 3 pending operations | |
| 2 | Kill the app (force stop) | | |
| 3 | Reopen the app | | |
| 4 | Check sync badge | Still shows "3 بانتظار المزامنة" | |
| 5 | Connect to internet | Sync runs automatically | |
| 6 | All 3 customers sync | Badge returns to green | |

#### 2.5 Slow Network (3G / Throttled)
| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|-----------|
| 1 | Connect to 3G or throttle to 1 Mbps | | |
| 2 | Login | Succeeds (may take 3-5s) | |
| 3 | Add customer | Saved locally, sync attempts | |
| 4 | Sync completes within 30s | No timeout errors | |
| 5 | Open map | Map tiles load (slowly but successfully) | |

#### 2.6 Network Switch (WiFi → 4G → WiFi)
| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|-----------|
| 1 | On WiFi, add customer | Syncs immediately | |
| 2 | Switch to 4G mid-sync | No crash, sync retries | |
| 3 | Add another customer | Syncs on 4G | |
| 4 | Switch back to WiFi | No issues | |

---

### 3. Map / Navigation Tests

#### 3.1 Map Display
| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|-----------|
| 1 | Open Home screen | Map loads within 2s | |
| 2 | Driver marker (blue) appears | At current GPS location | |
| 3 | Customer markers (teal) appear | At saved coordinates | |
| 4 | Tap "موقعك الحالي" FAB | Camera centers on driver | |
| 5 | Pan + zoom map | Smooth, no jank (< 60fps) | |

#### 3.2 Marker Clustering (50+ Customers)
| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|-----------|
| 1 | Add 50+ customers (or use seed data) | | |
| 2 | Zoom out on map | Clusters appear (count badges) | |
| 3 | Zoom in | Clusters split into individual markers | |
| 4 | Tap cluster | Camera zooms to cluster area | |

#### 3.3 Route Calculation
| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|-----------|
| 1 | Start delivery (ON_THE_WAY) | Route auto-calculates | |
| 2 | Route info card shows distance + ETA | e.g. "3.7 km" / "9 min" | |
| 3 | If offline → fallback "≈ 3.7 km" / "≈ 7 min" | "مسار تقريبي" hint shown | |

#### 3.4 Turn-by-Turn Navigation
| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|-----------|
| 1 | Tap "الملاحة" on Active Delivery | Navigation screen opens | |
| 2 | Instruction banner shows first maneuver | Arabic text (e.g. "انعطف يمينًا") | |
| 3 | Voice announcement plays | Arabic TTS speaks the instruction | |
| 4 | Drive along route | Instructions update at each turn | |
| 5 | Deviate from route | "جاري إعادة حساب المسار" appears | |
| 6 | New route calculated automatically | Navigation resumes | |
| 7 | Arrive at destination | "وصلت إلى وجهتك" + "تم التسليم" button | |

---

### 4. Delivery Flow Tests

#### 4.1 Full Delivery Lifecycle
| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|-----------|
| 1 | Customer Details → "بدء التوصيل" | Delivery created (ON_THE_WAY) | |
| 2 | Active Delivery screen shows status banner | Orange "في الطريق" | |
| 3 | Route + ETA shown | Distance + duration displayed | |
| 4 | Tap "وصلت إلى الزبون" | Status → ARRIVED (blue banner) | |
| 5 | Tap "تم التسليم" | Status → DELIVERED (green banner) | |
| 6 | Screen auto-pops to Home | | |
| 7 | History screen shows delivery under "اليوم" | ✅ icon, time, ID | |

#### 4.2 Cancel Delivery
| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|-----------|
| 1 | Start delivery (ON_THE_WAY) | | |
| 2 | Tap "إلغاء التوصيل" | Confirmation dialog | |
| 3 | Tap "إلغاء" | Status → CANCELLED (red banner) | |
| 4 | Screen auto-pops | | |
| 5 | History shows delivery with ❌ icon | | |

#### 4.3 Duplicate Active Delivery Prevention
| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|-----------|
| 1 | Start delivery for Customer A | ON_THE_WAY | |
| 2 | Go to Customer A details | "بدء التوصيل" creates second delivery | |
| 3 | Backend returns 409 CONFLICT | Error message shown | |
| 4 | Complete first delivery | | |
| 5 | Now start delivery for Customer A again | Succeeds (no active delivery) | |

---

### 5. Battery / Performance Tests

#### 5.1 Battery Consumption (1 Hour Active Use)
| Metric | Target | Actual | Pass/Fail |
|--------|--------|--------|-----------|
| 1 hour active use (map + navigation) | < 15% | | |
| 1 hour idle (Home screen, no delivery) | < 5% | | |
| Background (app minimized, no navigation) | < 2% | | |

#### 5.2 Memory Usage
| Metric | Target | Actual | Pass/Fail |
|--------|--------|--------|-----------|
| Home screen (100 customers) | < 150 MB | | |
| Navigation active | < 200 MB | | |
| After 10 deliveries (no leak) | < 200 MB | | |

#### 5.3 App Startup
| Metric | Target | Actual | Pass/Fail |
|--------|--------|--------|-----------|
| Cold start (first launch) | < 2s | | |
| Warm start (return from background) | < 500ms | | |

#### 5.4 Search Performance
| Metric | Target | Actual | Pass/Fail |
|--------|--------|--------|-----------|
| 100 customers, search by name | < 50ms | | |
| 1000 customers, search by name | < 100ms | | |
| 5000 customers, search by name | < 200ms | | |

---

### 6. UI / UX Tests

#### 6.1 RTL Layout
| Screen | RTL Correct? | Notes | Pass/Fail |
|--------|-------------|-------|-----------|
| Login | | | |
| Home (map + search + FABs) | | | |
| Customer List | | | |
| Add Customer | | | |
| Customer Details | | | |
| Active Delivery | | | |
| Navigation | | | |
| History | | | |
| Settings | | | |

#### 6.2 Dark / Light Mode
| Screen | Dark OK? | Light OK? | Pass/Fail |
|--------|---------|-----------|-----------|
| All screens | | | |

#### 6.3 Empty States
| Screen | Empty State Shown? | Pass/Fail |
|--------|-------------------|-----------|
| Customer List (no customers) | "لا يوجد زبائن بعد" | |
| History (no deliveries) | "لا توجد توصيلات في هذه الفترة" | |
| Home (no customers) | "أضف زبونًا جديدًا للبدء" | |

#### 6.4 Error States
| Scenario | Error Message? | Pass/Fail |
|----------|---------------|-----------|
| Login with wrong password | "اسم المستخدم أو كلمة المرور غير صحيحة" | |
| Backend down (500) | "خطأ في السيرفر. حاول لاحقًا." | |
| No internet (API call) | "لا يوجد اتصال بالإنترنت." | |

---

### 7. Sync Tests

#### 7.1 Sync Conflict (Multi-Device)
| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|-----------|
| 1 | Device A: add customer "محمد" | Synced to server | |
| 2 | Device B: sync → server_change received | "محمد" appears in Room | |
| 3 | Device A: rename to "محمد علي" | Synced | |
| 4 | Device B: sync → server_change (UPDATE) | Name updated in Room | |

#### 7.2 Sync Idempotency (Complete Delivery Retry)
| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|-----------|
| 1 | Complete delivery offline | PENDING sync | |
| 2 | Connect → sync starts | | |
| 3 | Kill app mid-sync | | |
| 4 | Reopen app → sync retries | | |
| 5 | Verify backend: only ONE DELIVERED | No double-completion | |

#### 7.3 Sync Queue Persistence
| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|-----------|
| 1 | Add 5 customers offline | 5 pending ops | |
| 2 | Force-stop the app | | |
| 3 | Reopen | 5 pending ops still there | |
| 4 | Connect → sync | All 5 sync successfully | |

---

### 8. Settings Tests

#### 8.1 Theme Toggle
| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|-----------|
| 1 | Settings → Dark | App immediately turns dark | |
| 2 | Settings → Light | App immediately turns light | |
| 3 | Settings → System | Follows system setting | |
| 4 | Restart app | Selected theme persists | |

#### 8.2 GPS Threshold + Arrival Radius
| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|-----------|
| 1 | Set GPS threshold to 5m | | |
| 2 | Add customer with 7m accuracy | Rejected (poor accuracy) | |
| 3 | Set arrival radius to 100m | | |
| 4 | Start delivery, stand 80m away | Arrival detected (80 < 100) | |

#### 8.3 Logout
| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|-----------|
| 1 | Settings → "تسجيل الخروج" | Confirmation dialog | |
| 2 | Tap "خروج" | App navigates to Login | |
| 3 | Reopen app | Goes to Login (session cleared) | |
| 4 | Login again | Fresh session, no stale data | |

---

## Test Sign-Off

| Tester | Date | Device | Result | Notes |
|--------|------|--------|--------|-------|
| | | | ☐ Pass ☐ Fail | |
| | | | ☐ Pass ☐ Fail | |

### Sign-Off Criteria
- [ ] All GPS tests pass (1.1-1.6)
- [ ] All Network/Offline tests pass (2.1-2.6)
- [ ] All Map/Navigation tests pass (3.1-3.4)
- [ ] All Delivery Flow tests pass (4.1-4.3)
- [ ] All Battery/Performance targets met (5.1-5.4)
- [ ] All UI/UX tests pass (6.1-6.4)
- [ ] All Sync tests pass (7.1-7.3)
- [ ] All Settings tests pass (8.1-8.3)
- [ ] No crashes during any test
- [ ] No ANRs during any test
- [ ] Battery consumption within targets

**Approved by:** _______________ **Date:** ___________
