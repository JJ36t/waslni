# وصلني (WASLNI) — Product Requirements Document

> **النسخة:** 1.0 (MVP Planning)
> **التاريخ:** 2026-09-08
> **الحالة:** Planning — جاهز للتطوير

---

## 1. نظرة عامة (Overview)

**وصلني** هو تطبيق Android مخصص للمندوب (Driver) يساعده على:
- حفظ بيانات الزبائن (الاسم + رقم الموبايل + إحداثيات GPS دقيقة).
- عرض الزبائن على الخريطة.
- الوصول للزبون بأفضل طريق عبر الملاحة.
- إدارة عدة توصيلات.
- إعطاء تنبيهات أثناء الطريق.
- تتبع الزبائن الذين تم توصيلهم.
- العمل Offline First ومزامنة البيانات عند عودة الإنترنت.
- الاحتفاظ بسجل التوصيلات.
- حساب إحصائيات أداء المندوب لاحقًا.

### القاعدة الأساسية للمشروع
> **لا يوجد عنوان نصي للزبون.**
> `name + phone + latitude + longitude + accuracy = بيانات الزبون الأساسية`
> الإحداثيات هي عنوان الزبون.

---

## 2. الأهداف (Goals)

### أهداف الـ MVP
1. تطبيق Android يعمل بسلاسة على أجهزة حقيقية.
2. التقاط موقع GPS بدقة عالية وإظهار جودة الدقة للمندوب.
3. حفظ الزبائن محليًا والعمل بدون إنترنت.
4. مزامنة البيانات تلقائيًا عند عودة الإنترنت.
5. ملاحة واضحة من موقع المندوب إلى موقع الزبون.
6. تسجيل حالة التوصيلات والاحتفاظ بسجل يومي/أسبوعي/شهري.

### غير أهداف الـ MVP (Non-Goals)
- لوحة تحكم Admin (مرحلة لاحقة).
- Route Optimization لعدة زبائن (مرحلة لاحقة).
- Live Tracking للمندوبين من السيرفر (مرحلة لاحقة).
- إحصائيات الأرباح (مرحلة لاحقة).
- Push Notifications من السيرفر (مرحلة لاحقة).

---

## 3. المستخدمون والأدوار (Users & Roles)

### في الـ MVP
| الدور | الوصف |
|------|------|
| **Driver** | المندوب — المستخدم الأساسي للتطبيق. يضيف الزبائن، يبدأ التوصيلات، يكملها، يرى سجله. |

### في مراحل لاحقة
| الدور | الوصف |
|------|------|
| **Admin** | مدير النظام — يدير المندوبين والزبائن والتوصيلات والتقارير. |

---

## 4. حالات الاستخدام للـ MVP (Use Cases)

### 4.1 الحساب (Authentication)
- **UC-01**: تسجيل الدخول بـ Username + Password.
- **UC-02**: تسجيل الخروج.
- **UC-03**: بقاء الجلسة فعالة عبر Refresh Token.

### 4.2 الزبائن (Customers)
- **UC-04**: إضافة زبون (اسم + رقم + GPS).
- **UC-05**: تعديل بيانات زبون (الاسم/الرقم).
- **UC-06**: تحديث موقع زبون (GPS جديد).
- **UC-07**: حذف زبون.
- **UC-08**: البحث بالاسم.
- **UC-09**: البحث بالرقم.
- **UC-10**: عرض الزبون على الخريطة.
- **UC-11**: الاتصال بالزبون.
- **UC-12**: كشف الزبائن المكررين بالرقم.

### 4.3 التوصيل (Delivery)
- **UC-13**: اختيار زبون وبدء التوصيل.
- **UC-14**: حساب المسار وعرض ETA.
- **UC-15**: الملاحة turn-by-turn.
- **UC-16**: إعادة حساب المسار عند الانحراف.
- **UC-17**: كشف الوصول (Arrival Detection).
- **UC-18**: إكمال التوصيل.
- **UC-19**: إلغاء التوصيل.
- **UC-20**: عرض سجل التوصيلات.

### 4.4 Offline & Sync
- **UC-21**: إضافة/تعديل/حذف زبون بدون إنترنت.
- **UC-22**: إنشاء/إكمال توصيل بدون إنترنت.
- **UC-23**: مزامنة تلقائية عند عودة الإنترنت.
- **UC-24**: معالجة التعارضات (Conflict Resolution).
- **UC-25**: منع التكرار في العمليات الحرجة (Idempotency).

### 4.5 الخريطة والملاحة (Maps & Navigation)
- **UC-26**: عرض موقع المندوب الحالي.
- **UC-27**: عرض جميع الزبائن كـ Markers.
- **UC-28**: تنبيهات الطريق (Turn alerts, arrival alert).
- **UC-29**: عرض حالة الشبكة (Online/Offline).

---

## 5. متطلبات المنتج التفصيلية (Functional Requirements)

### 5.1 بيانات الزبون (Customer Data Model)

```
Customer
├── id            (UUID, Primary Key)
├── name          (String, 2-120 chars, required)
├── phone         (String, 7-30 chars, required, unique per driver)
├── latitude      (Decimal, -90 → 90, 7 decimal places)
├── longitude     (Decimal, -180 → 180, 7 decimal places)
├── accuracy      (Float, meters, nullable)
├── created_at    (Timestamp)
└── updated_at    (Timestamp)
```

**ممنوع إضافة:** `street`, `house_number`, `description`, `address`.

### 5.2 بيانات التوصيل (Delivery Data Model)

```
Delivery
├── id            (UUID, Primary Key)
├── customer_id   (UUID, FK → customers)
├── driver_id     (UUID, FK → users)
├── status        (Enum: PENDING, ASSIGNED, ON_THE_WAY, ARRIVED, DELIVERED, CANCELLED)
├── created_at    (Timestamp)
├── started_at    (Timestamp, nullable)
├── arrived_at    (Timestamp, nullable)
├── completed_at  (Timestamp, nullable)
└── cancelled_at  (Timestamp, nullable)
```

### 5.3 حالات التوصيل (Delivery State Machine)

```
PENDING
   ↓
ASSIGNED
   ↓
ON_THE_WAY
   ↓
ARRIVED
   ↓
DELIVERED
```

أو:
```
PENDING → CANCELLED
```

**قواعد الانتقال:**
- `PENDING → DELIVERED`: ممنوع (قفزة غير منطقية).
- `DELIVERED → أي حالة`: ممنوع (حالة نهائية).
- `CANCELLED → أي حالة`: ممنوع (حالة نهائية، ما عدا إعادة إنشاء).

### 5.4 نظام دقة الموقع (GPS Accuracy)

عند التقاط الموقع نخزن:
- `latitude`
- `longitude`
- `accuracy` (بالأمتار)
- `timestamp`

**Threshold ابتدائي:**
- `accuracy ≤ 10m` → مقبول.
- `accuracy > 10m` → نطلب من المندوب الانتظار/إعادة الالتقاط.

> **ملاحظة:** الـ threshold قابل للتعديل بعد الاختبارات الفعلية على أرض الواقع. القيمة 10m ليست قاعدة مطلقة.

**السلوك عند الدقة السيئة:**
```
⚠️ دقة الموقع منخفضة
الدقة الحالية: 70m
انتظر لحظة وحاول مرة أخرى.
```

### 5.5 نظام المزامنة (Sync)

كل عملية محلية لها سجل في `sync_operations`:

```
SyncOperation
├── id            (UUID)
├── entity_id     (UUID, معرّف الكيان المتأثر)
├── operation     (Enum: CREATE_CUSTOMER, UPDATE_CUSTOMER, DELETE_CUSTOMER,
│                        CREATE_DELIVERY, UPDATE_DELIVERY, COMPLETE_DELIVERY)
├── payload       (JSON, البيانات المرسلة)
├── created_at    (Timestamp)
├── retry_count   (Int)
└── status        (Enum: PENDING, SYNCING, SYNCED, FAILED)
```

### 5.6 معالجة التعارضات (Conflict Resolution)

- **بيانات الزبائن (name, phone, location):** Latest-write-wins بناءً على `updated_at`.
- **إكمال التوصيل:** Idempotency-Key لمنع التكرار.

### 5.7 كشف التكرار (Duplicate Detection)

عند إضافة زبون جديد، نبحث برقم الهاتف:
- إذا موجود: نعرض السجل الموجود بدل إنشاء نسخة ثانية.
- الرسالة: `"يوجد زبون مسجل بهذا الرقم."`

---

## 6. متطلبات غير وظيفية (Non-Functional Requirements)

### 6.1 الأداء (Performance)
| المقياس | الهدف |
|--------|------|
| App Startup | < 2 ثانية |
| Map Load | < 1.5 ثانية |
| Search (محلي) | < 100ms لـ 5,000 زبون |
| API Response | < 500ms (95th percentile) |
| Sync (50 عملية) | < 10 ثواني |

### 6.2 Offline
- إضافة/تعديل/حذف الزبائن بدون إنترنت ✓
- إنشاء/إكمال التوصيلات بدون إنترنت ✓
- البحث المحلي بدون إنترنت ✓
- الملاحة تحتاج إنترنت (في الـ MVP — Offline Maps لاحقًا).

### 6.3 الأمان (Security)
- HTTPS إجباري.
- JWT (Access + Refresh Token).
- كلمات المرور بـ Argon2id أو bcrypt.
- لا تخزين كلمات المرور أو JWT في SharedPreferences العادية.
- Rate Limiting على endpoints الحساسة.
- Parameterized Queries (SQLAlchemy ORM).
- لا Secrets في Git.
- Input Validation على كل endpoint.
- Authorization: المندوب A لا يستطيع الوصول لزبائن المندوب B.

### 6.4 الخصوصية (Privacy)
- أقل قدر ممكن من البيانات (Minimal Collection).
- تشفير النقل (HTTPS/TLS).
- Logs بدون بيانات حساسة (Phone, Tokens, Passwords, Locations).
- صلاحيات واضحة على قاعدة البيانات.
- حذف البيانات عند الحاجة.

### 6.5 البطارية (Battery)
- عند عدم وجود توصيل: تحديثات موقع منخفضة التردد.
- أثناء الملاحة: تحديثات موقع أعلى تردد.
- بعد إنهاء التوصيل: إيقاف التتبع النشط.

### 6.6 الصلاحيات (Permissions)
- `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION` (مطلوبة).
- `POST_NOTIFICATIONS` (API 33+).
- `ACCESS_BACKGROUND_LOCATION` (لاحقًا، فقط عند الحاجة).

### 6.7 التوافق (Compatibility)
- **Min SDK:** 24 (Android 7.0) — يغطي ~97% من الأجهزة.
- **Target SDK:** آخر إصدار مستقر.
- **اللغة:** عربية RTL افتراضيًا.
- **Dark Mode + Light Mode.**

---

## 7. رحلة المستخدم (User Flow)

### 7.1 الرحلة الكاملة اليومية
```
فتح التطبيق
     ↓
Login (Username + Password)
     ↓
Home (الخريطة الرئيسية)
     ↓
+ إضافة زبون
     ↓
اسم + رقم
     ↓
التقاط GPS
     ↓
فحص الدقة
     ↓
تأكيد الموقع
     ↓
حفظ
     ↓
الزبون يظهر على الخريطة
     ↓
اختيار الزبون
     ↓
Customer Details
     ↓
[ بدء التوصيل ]
     ↓
حساب المسار (Routing)
     ↓
Navigation (Turn-by-turn)
     ↓
تنبيهات الطريق
     ↓
الوصول
     ↓
[ تم التسليم ]
     ↓
حفظ العملية
     ↓
سجل التوصيلات
     ↓
(عند عودة الإنترنت) Sync تلقائي
```

### 7.2 رحلة إضافة زبون
```
+ Add Customer
       ↓
Name (مطلوب, 2-120 chars)
       ↓
Phone (مطلوب, 7-30 chars)
       ↓
Get GPS (طلب Permission إن لزم)
       ↓
Check Accuracy
   ├── Good (≤ 10m) → Continue
   └── Bad (> 10m) → Retry
       ↓
Show Pin on Map
       ↓
Confirm Location
       ↓
Save to Room
       ↓
Add to Sync Queue
       ↓
(عند توفر الإنترنت) Sync to Backend
```

### 7.3 رحلة التوصيل
```
اختيار زبون
     ↓
[ بدء التوصيل ]
     ↓
Create Delivery (status=ON_THE_WAY, started_at=now)
     ↓
Get Current Driver Location
     ↓
Calculate Route (Routing API)
     ↓
Display: Distance + ETA
     ↓
Start Navigation (Turn-by-turn)
     ↓
Road Alerts (500m → Turn Right, ...)
     ↓
Re-routing عند الانحراف
     ↓
Arrival Detection (distance ≤ arrivalRadius)
     ↓
[ تم التسليم ]
     ↓
status=DELIVERED, completed_at=now
     ↓
Add to History
```

---

## 8. الصفحات (Screens)

| # | الصفحة | الوصف |
|---|-------|------|
| 1 | Splash | شاشة بداية + فحص الجلسة |
| 2 | Login | تسجيل الدخول |
| 3 | Home | الخريطة الرئيسية + البحث + إحصائيات سريعة |
| 4 | Customers | قائمة الزبائن + البحث |
| 5 | Add Customer | نموذج إضافة + التقاط GPS |
| 6 | Edit Customer | تعديل بيانات/موقع |
| 7 | Customer Details | تفاصيل + أزرار (توصيل/اتصال/تعديل/حذف) |
| 8 | Delivery | تفاصيل التوصيل الجاري |
| 9 | Navigation | الملاحة turn-by-turn |
| 10 | History | سجل التوصيلات بفلترة (اليوم/الأسبوع/الشهر) |
| 11 | Settings | إعدادات + تسجيل خروج |

---

## 9. واجهة المستخدم (UI/UX Principles)

- **عربية RTL** افتراضيًا.
- **بسيطة جدًا** — المندوب ما عنده وقت يتفلسف مع التطبيق.
- **أزرار كبيرة** مناسبة للاستخدام أثناء العمل.
- **Dark/Light Mode.**
- **أقل عدد ممكن من الخطوات** — إضافة الزبون 3 خطوات فقط:
  1. الاسم + الرقم
  2. تحديد الموقع
  3. حفظ
- **الخريطة هي العنصر الرئيسي** في Home.
- **رسائل أخطاء واضحة** — لا نعرض `Exception 500 Retrofit IOException`، نعرض:
  `"تعذر الاتصال بالسيرفر، تم حفظ العملية وسيتم مزامنتها لاحقًا."`

### Home Screen Layout
```
┌─────────────────────────────────┐
│ 🔍 ابحث عن زبون                  │
├─────────────────────────────────┤
│                                 │
│              🟢                 │
│                                 │
│      📍            📍           │
│                                 │
│                  📍             │
│                                 │
│         🔵                      │
│                                 │
├─────────────────────────────────┤
│  📦 اليوم: 12    ⏳ متبقي: 5    │
├─────────────────────────────────┤
│  🗺️      👥      📦      ⚙️    │
└─────────────────────────────────┘
```

### Add Customer Screen Layout
```
┌────────────────────────────────────┐
│           إضافة زبون                │
│                                    │
│ اسم الزبون                          │
│ [ محمد أحمد                    ]   │
│                                    │
│ رقم الموبايل                        │
│ [ 07801234567                  ]   │
│                                    │
│ الموقع                              │
│                                    │
│          🗺️ الخريطة                 │
│                📍                  │
│                                    │
│ الدقة: 4m ✓                        │
│                                    │
│ [          حفظ الزبون          ]   │
└────────────────────────────────────┘
```

### Customer Details Layout
```
┌────────────────────────────────────┐
│  محمد أحمد                          │
│  07801234567                        │
│                                    │
│  📍                                 │
│  31.978942                          │
│  44.940127                          │
│                                    │
│  GPS Accuracy: 4m                   │
│  Last Updated: 07/09/2026           │
│                                    │
│  [ 🧭 بدء التوصيل ]                 │
│  [ 📞 اتصال        ]                │
│  [ ✏️ تعديل        ]                │
│  [ 🗑️ حذف          ]                │
└────────────────────────────────────┘
```

---

## 10. إدارة الأخطاء (Error Handling)

كل Feature له 4 حالات واضحة:
- **Loading** — جاري التحميل.
- **Success** — تم بنجاح.
- **Empty** — لا توجد بيانات.
- **Error** — فشل + زر Retry.

### رسائل الأخطاء المعتمدة
| الخطأ | الرسالة |
|------|--------|
| No Internet | `"لا يوجد اتصال بالإنترنت. تم حفظ العملية محليًا وسيتم مزامنتها لاحقًا."` |
| Server Error | `"تعذر الاتصال بالسيرفر. حاول مرة أخرى لاحقًا."` |
| Unauthorized | `"انتهت الجلسة. الرجاء تسجيل الدخول."` |
| Validation | `"بيانات غير صحيحة: [التفاصيل]"` |
| GPS Disabled | `"الـGPS معطل. الرجاء تفعيله من الإعدادات."` |
| Permission Denied | `"يحتاج التطبيق إلى صلاحية الموقع لتحديد موقعك. يمكنك منحها من الإعدادات."` |
| Poor Accuracy | `"دقة الموقع منخفضة (XXm). انتظر وحاول مرة أخرى."` |

---

## 11. قيود الـ MVP (MVP Constraints)

| ✅ في الـ MVP | ❌ خارج الـ MVP |
|-------------|----------------|
| إدارة الزبائن (CRUD) | Route Optimization لعدة زبائن |
| التقاط GPS + فحص الدقة | Live Tracking من السيرفر |
| عرض الزبائن على الخريطة | Admin Panel |
| ملاحة لزبون واحد | Multi-Stop Route |
| إدارة التوصيل (state machine) | Earnings / أرباح |
| سجل التوصيلات | Analytics متقدمة |
| Offline First (Room) | Offline Maps (تحتاج Map SDK يدعمها) |
| Sync تلقائي | Push Notifications |
| Authentication (Username + Password) | OTP / SMS Auth |
| كشف التكرار بالرقم | Customer Merge |
| تنبيهات محلية أثناء الملاحة | FCM Push |

---

## 12. معايير القبول (Acceptance Criteria)

### معايير قبول الـ MVP
- [ ] المندوب يستطيع تسجيل الدخول والخروج.
- [ ] المندوب يستطيع إضافة زبون مع GPS دقة ≤ 10m.
- [ ] المندوب يستطيع رؤية جميع زبائنه على الخريطة.
- [ ] المندوب يستطيع البحث بالاسم والرقم.
- [ ] المندوب يستطيع تعديل بيانات/موقع زبون.
- [ ] المندوب يستطيع حذف زبون.
- [ ] المندوب يستطيع بدء توصيل والملاحة إلى الزبون.
- [ ] المندوب يستطيع إكمال/إلغاء توصيل.
- [ ] المندوب يستطيع رؤية سجل التوصيلات.
- [ ] كل العمليات السابقة تعمل Offline.
- [ ] المزامنة تتم تلقائيًا عند عودة الإنترنت.
- [ ] لا تضيع أي عملية عند إغلاق التطبيق وإعادة فتحه.
- [ ] التطبيق يعمل بسلاسة على جهاز حقيقي.
- [ ] Dark/Light Mode يعملان.
- [ ] RTL يعمل بشكل صحيح.

---

## 13. المخاطر والافتراضات (Risks & Assumptions)

### مخاطر
| الخطر | التأثير | التخفيف |
|------|--------|--------|
| ضعف دقة GPS في بعض المناطق | بيانات موقع غير دقيقة | threshold قابل للتعديل + إعادة الالتقاط |
| انقطاع الإنترنت المتكرر | تأخر المزامنة | Offline First + WorkManager retry |
| استهلاك البطارية العالي | شكوى المستخدمين | تحكم في تردد تحديثات الموقع |
| تعقيد Conflict Resolution | فقدان بيانات | Latest-write-wins + Idempotency |
| ضعف تغطية Mapbox في العراق | ملاحة سيئة | اختبار ميداني + خطة بديلة (OSM/Google) |

### افتراضات
- المندوب يستخدم جهاز Android 7.0+.
- المندوب لديه حساب تم إنشاؤه له (Admin ينشئه في الـ MVP).
- GPS الجهاز يعمل بشكل صحيح.
- خادم Backend متاح بصورة مستقرة.

---

## 14. المراجع (References)

- وثيقة Architecture: `02-architecture.md`
- تصميم قاعدة البيانات: `03-database.md`
- API Contract: `04-api-contract.md`
- هيكل المشروع و Git: `05-project-structure.md`
- خطة التنفيذ: `06-build-phases.md`

---

**نهاية وثيقة PRD — WASLNI MVP v1.0**
