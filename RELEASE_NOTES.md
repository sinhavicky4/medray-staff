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
