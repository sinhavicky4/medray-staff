# MedRay Staff Version 0.2.0 Release Notes

- **➕ Add Registered Patient to Doctor's Queue**: Staff can now directly add any existing registered patient from the directory to any consulting doctor's queue with instant token generation, doctor selection, chief complaint, and optional vitals.
- **🔄 Tabbed Walk-In & Existing Patient Registration**: Enhanced Walk-In dialog supporting instant patient lookup & selection as well as new walk-in creation.
- **📱 Dynamic Clinic UPI ID Integration**: Wired clinic-specific UPI ID (VPA) from web/API into dynamic on-screen QR codes and payment ledger recording.
- **⬇️ Pull-Down to Refresh**: Added smooth pull-to-refresh swipe gesture across OPD Queue, Patients Directory, and Billing screens.

# MedRay Staff Version 0.1.7 Release Notes

- **🔢 FIFO Queue Ordering & Completed Deprioritization**: OPD queue records are now strictly sorted by First-In, First-Out (FIFO) arrival order with completed and cancelled records automatically pushed to the bottom.
- **🔄 Optimized Local SQLite Queue DAO**: Updated Room DAO queries with SQL CASE priority ordering for instant offline-first rendering.

# MedRay Staff Version 0.1.6 Release Notes

- **📅 Redesigned Appointments Schedule**: Doctor Tablet aesthetic parity with 2x2 interactive KPI stat cards (Scheduled, Checked In, Completed, Cancelled), time slot badges, assigned consulting doctors, quick status filter pills, and direct one-tap "Check In to Queue" workflow.
- **💳 Redesigned Billing & Payments**: Real-time revenue overview (Total Collected & Pending Due KPI counters), invoice status badges, patient UHID links, and instant dynamic on-screen UPI QR payment collection.
- **⚙️ Redesigned Staff Profile**: Elevated clinical profile card with staff initials squircle, verified role badges, full assigned clinic details with UPI VPA configuration, 256-bit encryption compliance assurance, and confirmed sign-out dialog.

# MedRay Staff Version 0.1.5 Release Notes

- **📋 Doctor App Parity OPD Queue Redesign**: 100% synchronized with the Doctor Tablet App featuring clinical slate backgrounds (`#F8FAFC`), pure white cards with `1.dp` border `#E2E8F0`, contextual header greetings, and 2x2 interactive KPI stat cards (`Arrived`, `Waiting`, `In Triage`, `Completed`).
- **⏱️ Timestamp & Creator Attribution Badges**: Dedicated badges on every queue card indicating exact check-in timestamp and staff creator attribution (e.g. `⏱️ 4:20 PM · by Front Desk`).
- **👥 Redesigned Patients Directory (`PatientsScreen.kt`)**: Modern patient records directory with interactive KPI counters (Total, Male, Female, Seniors 60+), live search, quick filter pills, initial avatars, UHID tags, verified phone contacts, and detailed patient clinical modals.
- **🔄 Room Database Schema v2**: Seamless local database schema migration with automatic data integrity handling.

# MedRay Staff Version 0.1.4 Release Notes

- **🌐 Resilient Google Sign-In with Play Services Fallback**: Added Google Play Services `GoogleSignInClient` Intent launcher fallback to support full-screen Google Account selector and "Add another account" flow on all Android devices.
- **🛠️ Enhanced Error Transparency**: Context-aware diagnostics for account registration and configuration.

# MedRay Staff Version 0.1.3 Release Notes

- **🌊 Animated Clinical Splash Screen**: Synchronized with Doctor Tablet App featuring breathing squircle logo, Plus Jakarta Sans branding, "NURSES & FRONT DESK MOBILE WORKSPACE" pill, flowing WavyBackground ribbons, and 256-bit encryption compliance assurance.
- **📱 100% Doctor App Parity Login Screen**: Matching mobile phone portrait design with country code pill (`🇮🇳 +91`), live numeric phone validation indicator, vibrant action button with `ArrowForward` icon, `OR CONTINUE WITH` divider, and Google Workspace button with official logo.
- **🎨 Shared Design Tokens & Typography**: Seamless integration of Google Fonts (`Plus Jakarta Sans` for titles and `Inter` for body copy), unified status pill badges, and slate surface elevations.
- **🛡️ Clear Google Sign-In Guidance**: Contextual error explanations informing users if their Google account needs to be registered by their Clinic Admin.

# MedRay Staff Version 0.1.2 Release Notes

- **🌐 Google Sign-In Integration**: Seamless 1-tap "Continue with Google" sign-in via Android Credential Manager and `/api/auth/google/mobile`.
- **🧭 Web-Parity Navigation Drawer (Sidebar)**: Complete off-canvas sliding sidebar menu accessible via top-bar hamburger button, featuring:
  - 📋 **OPD / Triage Queue**
  - 👥 **Patients Directory & Search**
  - 📅 **Appointments Schedule**
  - 💳 **Billing & Dynamic UPI QR Payments**
  - 📲 **Self Check-In Kiosk & Arrivals**
  - ⚙️ **Staff Profile & Clinic Details**
  - 🚪 **Sign Out & App Status**
- **✨ Polished Modern UI**: Elevated authentication card with brand styling, smooth segmented tabs, and responsive error banners.

# MedRay Staff Version 0.1.1 Release Notes

- **🔗 Production API Route Fix**: Connected directly to AWS Lambda & API Gateway production endpoints (`/api/auth/otp/request` & `/api/auth/otp/verify`).
- **🔐 Dual Staff Authentication**: Support for both instant Mobile OTP and Email / Password sign-in.
- **🛡️ Secure Cookie & CSRF Storage**: EncryptedSharedPreferences `SessionCookieJar` & `CsrfInterceptor` for zero-friction authenticated requests.
- **📱 Mobile-First Staff Experience**: Dedicated smartphone architecture for Nurses and Receptionists.
- **⚡ Rapid One-Handed Vitals Triage**: Numeric keypad for instant BP, Pulse, Temp, SpO2, and Weight entry with BMI computation.
- **💳 Dynamic UPI QR Payments**: On-screen QR generation for direct patient phone payment scanning.
- **➕ OPD Walk-In Registration**: Instant token issuance with automatic WhatsApp notifications.
- **⚡ 100% Offline-First Resilience**: Room SQLite database with WorkManager background outbox sync.
