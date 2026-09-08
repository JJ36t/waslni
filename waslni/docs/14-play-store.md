# WASLNI — Play Store Preparation

> Everything needed to publish the Waselni app on Google Play Store.

---

## 1. Store Listing

### App Name
```
وصلني — Waselni
```

### Short Description (80 chars max)
```
تطبيق المندوب: حفظ الزبائن بالموقع، ملاحة، توصيل، وعمل بدون إنترنت.
```

### Full Description (4000 chars max)
```
وصلني هو تطبيق المندوب الذي يساعدك على:

📍 حفظ الزبائن بإحداثيات GPS الدقيقة
لا حاجة لعناوين نصية — احفظ موقع الزبون مباشرة من GPS مع دقة الموقع.

🗺️ الخريطة والملاحة
عرض جميع زبائنك على الخريطة مع ملاحة turn-by-turn وإرشادات صوتية بالعربية.

📦 إدارة التوصيلات
ابدأ التوصيل، تأكد من الوصول، أكمل التوصيل — كل شيء مسجل في السجل اليومي.

📶 يعمل بدون إنترنت
أضف زبوناً، أكمل توصيلة، ابحث — كل شيء يعمل أوفلاين والمزامنة تتم تلقائياً عند عودة الإنترنت.

🔔 تنبيهات ذكية
تنبيه عند الاقتراب من الزبون، تنبيه عند الوصول، وإشعارات بحالة التوصيل.

🌙 الوضع الداكن
واجهة عربية RTL بسيطة وسريعة مع دعم الوضع الداكن والفاتح.

المميزات:
• حفظ الزبائن بالاسم ورقم الموبايل وإحداثيات GPS
• خريطة كاملة مع markers لكل زبون
• ملاحة turn-by-turn مع إرشادات صوتية
• حساب المسافة والوقت المتوقع (ETA)
• كشف الوصول التلقائي عند الاقتراب من الزبون
• سجل التوصيلات (يومي/أسبوعي/شهري) مع إحصائيات
• عمل كامل بدون إنترنت مع مزامنة تلقائية
• بحث سريع بالاسم أو رقم الهاتف
• كشف الزبائن المكررين برقم الهاتف
• تحديث موقع الزبون بإعادة التقاط GPS
• إشعارات محلية للوصول والإكمال
• وضع داكن/فاتح/حسب النظام
• إعدادات قابلة للتخصيص (حد دقة GPS، نطاق الوصول)

وصلني — بسيط، سريع، ويعمل دائماً.
```

### Developer Name
```
Waselni Team
```

### Developer Email
```
support@waslni.com
```

### Developer Website
```
https://waslni.com
```

### Privacy Policy URL
```
https://waslni.com/privacy-policy
```

---

## 2. Screenshots

### Required Screenshots (min 2, max 8)
| # | Screen | Description (EN) | Description (AR) |
|---|--------|-----------------|-----------------|
| 1 | Home (map) | Map with customer markers + driver location | الخريطة مع مواقع الزبائن |
| 2 | Add Customer | Form with GPS capture + accuracy indicator | إضافة زبون مع التقاط GPS |
| 3 | Customer Details | Customer info + start delivery button | تفاصيل الزبون + بدء التوصيل |
| 4 | Active Delivery | Status banner + route info + ETA | التوصيل النشط + معلومات الطريق |
| 5 | Navigation | Turn-by-turn instruction banner | الملاحة مع إرشادات الاتجاه |
| 6 | History | Stats card + delivery list | سجل التوصيلات مع الإحصائيات |
| 7 | Offline mode | Sync badge showing "pending sync" | وضع عدم الاتصال + المزامنة |
| 8 | Settings | Theme toggle + sliders | الإعدادات + المظهر |

### Screenshot Specs
- **Phone screenshots**: 1080 x 1920 px (16:9) or 1080 x 2400 px (20:9)
- **Format**: PNG or JPEG
- **Min**: 2 screenshots
- **Max**: 8 screenshots
- **Tablet** (optional but recommended): 1200 x 1920 px

### Feature Graphic
- **Size**: 1024 x 500 px
- **Format**: PNG or JPEG
- **Content**: Waselni logo + tagline "تطبيق المندوب" on brand teal background

### App Icon
- **Size**: 512 x 512 px
- **Format**: PNG (32-bit)
- **Content**: Location pin in brand teal (#0F766E) on white background

---

## 3. Categorization

| Field | Value |
|-------|-------|
| Category | Maps & Navigation |
| Content rating | Everyone |
| Target audience | 18+ (professional drivers) |
| Contains ads | No |
| In-app purchases | No |
| Application type | Applications |
| Pricing | Free |

---

## 4. Privacy Policy

### Summary (for Play Store)
Waselni collects the following data to provide its delivery navigation services:
- **Account data**: username (no email required)
- **Customer data**: name, phone number, GPS coordinates (added by the driver)
- **Delivery data**: status, timestamps
- **Location data**: driver's GPS location during active deliveries
- **Device data**: crash reports (via Firebase Crashlytics), device model, OS version

Waselni does NOT collect:
- Browsing history
- Contacts or SMS
- Photos or media files
- Biometric data
- Advertising identifiers

Data is stored securely:
- Tokens encrypted with AES-256-GCM (Android Keystore)
- Passwords hashed with Argon2id (server-side)
- All traffic over HTTPS with certificate pinning
- No data sold to third parties

Full privacy policy: https://waslni.com/privacy-policy

---

## 5. Data Safety Form (Play Console)

### Data Collected

| Data Type | Collected | Shared | Purpose | Encrypted | Deletable |
|-----------|----------|--------|---------|-----------|-----------|
| Personal info (name) | ✅ | ❌ | App functionality | ✅ | ✅ |
| Phone number | ✅ | ❌ | App functionality | ✅ | ✅ |
| Location (approximate) | ✅ | ❌ | App functionality (navigation) | ✅ | ✅ |
| Location (precise) | ✅ | ❌ | App functionality (GPS capture) | ✅ | ✅ |
| App activity (interactions) | ✅ | ❌ | Analytics (crash reports) | ✅ | ✅ |
| App info (crashes) | ✅ | ❌ | Analytics (Firebase Crashlytics) | ✅ | ✅ |
| Device ID | ✅ | ❌ | Analytics (crash correlation) | ✅ | ✅ |

### Data NOT Collected
- ❌ Financial info
- ❌ Health & fitness
- ❌ Messages
- ❌ Photos & videos
- ❌ Audio files
- ❌ Files & docs
- ❌ Calendar
- ❌ Contacts
- ❌ Web browsing
- ❌ Email
- ❌ User IDs (we use UUIDs, not Google/Microsoft IDs)
- ❌ Purchase history
- ❌ Advertising IDs

### Security Practices
- Data encrypted in transit: ✅ (HTTPS + TLS 1.2+)
- Data encrypted at rest: ✅ (AES-256-GCM for tokens, Argon2id for passwords)
- Users can request data deletion: ✅ (contact support@waslni.com)
- No data shared with third parties: ✅
- No ads: ✅
- No analytics SDKs except Firebase Crashlytics: ✅

---

## 6. Content Rating Questionnaire

| Question | Answer |
|----------|--------|
| Does the app contain cartoon violence? | No |
| Does the app contain realistic violence? | No |
| Does the app contain sexual content? | No |
| Does the app contain profanity? | No |
| Does the app contain controlled substances? | No |
| Does the app contain gambling? | No |
| Does the app share user location? | Yes |
| Does the app share user-generated content? | No |
| Does the app allow digital purchases? | No |
| Does the app allow user interactions? | No |

**Resulting rating: Everyone**

---

## 7. Target Audience

| Question | Answer |
|----------|--------|
| Target age group | 18+ |
| Is the app designed for children? | No |
| Does the app appeal to children? | No |

---

## 8. News & Updates (Release Notes)

### v1.0.0 — MVP Launch
```
الإصدار الأول من وصلني — تطبيق المندوب:

✨ المميزات:
• حفظ الزبائن بالاسم والرقم وإحداثيات GPS الدقيقة
• خريطة كاملة مع ملاحة turn-by-turn وإرشادات صوتية
• إدارة التوصيلات (بدء → وصول → إكمال)
• سجل التوصيلات اليومي/الأسبوعي/الشهري مع إحصائيات
• عمل كامل بدون إنترنت مع مزامنة تلقائية
• كشف الوصول التلقائي عند الاقتراب من الزبون
• بحث سريع بالاسم أو رقم الهاتف
• إشعارات ذكية للوصول والإكمال
• وضع داكن/فاتح/حسب النظام

🔧 التقنية:
• تطبيق Android أصلي (Kotlin + Jetpack Compose)
• خريطة Mapbox مع ملاحة احترافية
• قاعدة بيانات محلية (Room) — يعمل أوفلاين
• مزامنة تلقائية عبر WorkManager
• أمان: تشفير AES-256 + شهادات SSL مثبتة
```

---

## 9. Staged Rollout Plan

| Stage | Percentage | Duration | Monitor |
|-------|-----------|----------|---------|
| Internal testing | 20 testers | 3 days | Crashes, feedback, sync issues |
| Closed testing | 100 users | 5 days | Crash-free rate, ANR rate, battery |
| Open beta | 500 users | 5 days | All metrics + Play Store reviews |
| Production 10% | 10% | 3 days | Crash rate < 1%, no critical bugs |
| Production 50% | 50% | 3 days | All metrics stable |
| Production 100% | 100% | — | Full release |

### Monitoring During Rollout
- Firebase Crashlytics: crash-free rate > 99%
- ANR rate < 0.5%
- Play Console: review ratings, uninstall rate
- Backend: /metrics endpoint, error logs
- Support inbox: support@waslni.com

### Rollback Criteria
- Crash rate > 2% → halt rollout + investigate
- ANR rate > 1% → halt rollout
- Critical bug reported (data loss, sync failure) → halt + hotfix
- Average rating < 3.0 → halt + investigate

---

## 10. Play Store Upload Checklist

### Pre-Upload
- [ ] Release AAB built and signed (Phase 29)
- [ ] All release checklist items passed (Phase 29)
- [ ] Real Device Test Plan signed off (Phase 24)
- [ ] Production backend deployed and healthy
- [ ] Firebase Crashlytics verified (test crash appears in dashboard)
- [ ] Privacy Policy published at https://waslni.com/privacy-policy

### Store Listing
- [ ] App name: "وصلني — Waselni"
- [ ] Short description (80 chars)
- [ ] Full description (4000 chars)
- [ ] App icon (512 x 512 px PNG)
- [ ] Feature graphic (1024 x 500 px)
- [ ] Phone screenshots (min 2, up to 8)
- [ ] Developer name + email + website

### Categorization
- [ ] Category: Maps & Navigation
- [ ] Content rating: Everyone
- [ ] Target audience: 18+
- [ ] Contains ads: No
- [ ] In-app purchases: No
- [ ] Pricing: Free

### Data Safety
- [ ] All data types declared
- [ ] Encryption confirmed
- [ ] Data deletion request process documented

### Release
- [ ] Upload AAB to Play Console
- [ ] Release notes written
- [ ] Internal testing track: 20 testers
- [ ] Staged rollout: 10% → 50% → 100%
- [ ] Monitor crash rate + reviews during rollout
