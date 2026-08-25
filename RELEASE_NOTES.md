# MedRay Staff Version 0.1.1 Release Notes

- **🔗 Production API Route Fix**: Connected directly to AWS Lambda & API Gateway production endpoints (`/api/auth/otp/request` & `/api/auth/otp/verify`).
- **🔐 Dual Staff Authentication**: Support for both instant Mobile OTP and Email / Password sign-in.
- **🛡️ Secure Cookie & CSRF Storage**: EncryptedSharedPreferences `SessionCookieJar` & `CsrfInterceptor` for zero-friction authenticated requests.
- **📱 Mobile-First Staff Experience**: Dedicated smartphone architecture for Nurses and Receptionists.
- **⚡ Rapid One-Handed Vitals Triage**: Numeric keypad for instant BP, Pulse, Temp, SpO2, and Weight entry with BMI computation.
- **💳 Dynamic UPI QR Payments**: On-screen QR generation for direct patient phone payment scanning.
- **➕ OPD Walk-In Registration**: Instant token issuance with automatic WhatsApp notifications.
- **⚡ 100% Offline-First Resilience**: Room SQLite database with WorkManager background outbox sync.
