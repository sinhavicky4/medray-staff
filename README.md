# MedRay Staff — Mobile Android Application

The **MedRay Staff** Android app (`ai.medray.staff`) is a mobile-first application engineered for **Nurses** and **Receptionists** in outpatient clinics and hospitals.

---

## 🌟 Key Features

### 🩺 Nurse Persona
- **High-Speed Vitals Triage Pad**: Fast one-handed entry for BP, Pulse, Temperature, SpO2, Respiratory Rate, Height, and Weight with automatic BMI calculation and clinical abnormality detection.
- **CameraX Document Scanner**: Instant multi-page capture of physical lab reports, radiology films, and external medical records attached directly to the patient EHR.
- **Queue Synchronization**: Real-time sync with the Doctor Tablet App.

### 👩‍💼 Receptionist Persona
- **Walk-in OPD Registration**: Quick patient onboarding with atomic OPD token generation (`OPD-YYYYMMDD-NNN`) and automated WhatsApp token alerts.
- **Dynamic On-Screen UPI QR Billing**: Generates live NPCI-compliant UPI QR codes for patients to scan and pay directly using GPay, PhonePe, Paytm, or BHIM.
- **Appointment Scheduling**: Daily calendar view, slot booking, rescheduling, and 1-tap check-in with optional pre-consultation vitals.
- **1-Tap WhatsApp Integration**: Instant patient messaging and tax invoice sharing.

---

## 🛠️ Tech Stack
- **UI**: Kotlin 2.0, Jetpack Compose, Material3 Design System
- **Local DB**: Room SQLite + SQLCipher encryption
- **Offline Engine**: WorkManager Outbox with automatic cloud replay
- **Networking**: Retrofit2 + OkHttp3
- **Hardware Integration**: CameraX (Document scanner), ZXing (Dynamic UPI QR)
