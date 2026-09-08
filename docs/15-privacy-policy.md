# WASLNI — Privacy Policy

> Effective date: September 8, 2026

## Overview

Waselni ("وصلني") is a delivery navigation app for drivers. This Privacy Policy explains what data we collect, how we use it, and how we protect it.

## Data We Collect

### 1. Account Data
| Data | Purpose | Stored Where |
|------|---------|-------------|
| Username | Login | Backend (PostgreSQL) |
| Password (hashed) | Authentication | Backend (Argon2id hash — never plaintext) |

We do NOT collect email addresses, real names, or social media accounts.

### 2. Customer Data
| Data | Purpose | Stored Where |
|------|---------|-------------|
| Customer name | Identification | Backend + Local (Room) |
| Customer phone | Contact | Backend + Local (Room) |
| Customer GPS coordinates | Navigation | Backend + Local (Room) |
| GPS accuracy | Quality indicator | Backend + Local (Room) |

Customer data is entered by the driver and visible only to that driver.

### 3. Delivery Data
| Data | Purpose | Stored Where |
|------|---------|-------------|
| Delivery status | Tracking | Backend + Local (Room) |
| Timestamps (created, started, arrived, completed, cancelled) | History | Backend + Local (Room) |

### 4. Location Data
| Data | Purpose | Duration |
|------|---------|----------|
| Driver GPS (during active delivery) | Navigation + arrival detection | While delivery is ON_THE_WAY or ARRIVED |
| Driver GPS (home screen) | Map marker | 15-second intervals while app is foreground |

We do NOT track drivers in the background when the app is not in use.

### 5. Technical Data
| Data | Purpose | Stored Where |
|------|---------|-------------|
| Crash reports | Bug fixing | Firebase Crashlytics |
| Device model + OS version | Crash correlation | Firebase Crashlytics |
| App version | Crash correlation | Firebase Crashlytics |
| User UUID (not name/phone) | Crash correlation | Firebase Crashlytics |

## Data We Do NOT Collect

- ❌ Email addresses
- ❌ Contacts or address book
- ❌ SMS or call logs
- ❌ Photos or media files
- ❌ Browsing history
- ❌ Biometric data
- ❌ Advertising identifiers
- ❌ Financial information

## How We Use Data

1. **To provide the service**: store customers, calculate routes, navigate, track deliveries.
2. **To sync across devices**: your data is synced between your devices via the backend.
3. **To fix bugs**: crash reports help us identify and fix issues.
4. **To improve the app**: anonymous usage statistics (future — not currently collected).

We do NOT:
- Sell data to third parties
- Use data for advertising
- Share data with third parties (except Firebase Crashlytics for crash reporting)

## Data Security

| Measure | Implementation |
|---------|---------------|
| Transport encryption | HTTPS (TLS 1.2+) with certificate pinning |
| Token storage | AES-256-GCM (Android Keystore-backed) |
| Password hashing | Argon2id (OWASP-recommended parameters) |
| Database security | Parameterized queries (SQLAlchemy ORM) |
| Rate limiting | 5/min login, 100/min API |
| Security headers | HSTS, X-Content-Type-Options, X-Frame-Options |
| JWT | 15-min access + 30-day refresh (rotated) |
| ProGuard/R8 | Code obfuscation in release build |

## Data Retention

| Data | Retention |
|------|-----------|
| Account | Until account deletion requested |
| Customers | Until driver deletes them |
| Deliveries | Until driver deletes the related customer |
| Crash reports | 90 days (Firebase Crashlytics default) |
| Audit logs | 1 year (security requirement) |
| Sync operations | 7 days after successful sync |

## Your Rights

### Access
You can view your data in the app (customers, deliveries, history).

### Deletion
- **Delete a customer**: tap "حذف" in Customer Details.
- **Delete account**: contact support@waslni.com. We will delete your account, customers, deliveries, and tokens within 30 days.

### Export (future)
You will be able to export your customer list as CSV (planned for Phase 2+).

## Children's Privacy

Waselni is not designed for or directed at children. We do not knowingly collect data from children. If you believe a child has provided data, contact support@waslni.com.

## Third-Party Services

| Service | Purpose | Data Shared |
|---------|---------|-------------|
| Firebase Crashlytics | Crash reporting | Crash stack traces, device model, OS version, app version, user UUID |
| Mapbox | Maps + routing + navigation | GPS coordinates (for route calculation) |

Neither service receives customer phone numbers, passwords, or JWT tokens.

## Changes to This Policy

We may update this Privacy Policy. We will notify users of material changes via in-app notification. The "Effective date" at the top will reflect the last update.

## Contact

- Email: support@waslni.com
- Website: https://waslni.com
