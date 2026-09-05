# MedRay AI — Staff Capabilities: Web Platform vs. Mobile App

**Document Version:** 1.1  
**Target Roles:** Receptionist (Front Desk) & Nurse (Triage / Clinical Staff)  
**Target Systems:** `medray-platform` (Web Portal) & `medray-staff` (Android Mobile App)  
**Last Updated:** August 2026  

> **Accuracy note (2026-08-28):** this document was corrected against a full code audit. Several mobile capabilities described in the original version — camera-based document/photo capture, biometric sign-in, and 1-tap vitals presets — are not actually implemented (declared dependencies/manifest permissions exist, but no working code does). The AI Assistant row has also been updated: it shipped in v0.4.0, no longer "planned." For an exhaustive, code-verified feature inventory including every known gap, see `docs/FEATURES.md`.

---

## 1. Executive Summary

MedRay AI provides an integrated clinical operating system tailored for Outpatient Departments (OPD), polyclinics, and specialty practices. Clinical support staff—specifically **Receptionists (Front Desk Staff)** and **Nurses**—play a vital role in patient intake, triage, queue progression, billing, and document archiving.

Their operational surfaces are strategically divided between two complementary platforms:

1. **Web Platform (`medray-platform`)**: Designed for stationary desktop/laptop workstations (reception counter, billing desk, nursing station). Features expansive multi-column table layouts, high-volume data entry with physical keyboards, thermal/laser receipt printing, full staff directory administration, advanced revenue analytics, and an integrated AI Copilot chat assistant.
2. **Mobile App (`medray-staff`)**: Purpose-built for high-mobility clinical operations on Android smartphones. Features touch-first card interfaces, strict FIFO queue ordering, fast vitals entry with real-time severity alerts, 1-tap phone/WhatsApp calling, and offline-first Room database synchronization with automated Outbox recovery for queue/vitals/registration writes. (Camera-based document/photo capture and one-tap vitals presets are planned but not yet implemented — see §4.5 and `docs/FEATURES.md`.)

---

## 2. Summary Comparison Matrix

| Feature & Workflow Domain | 💻 Web Platform (`medray-platform`) | 📱 Mobile App (`medray-staff`) | Optimal Surface & Operational Context |
| :--- | :--- | :--- | :--- |
| **Target Device & Form Factor** | Desktop / Laptop (Mouse & Full Keyboard) | Android Smartphone (Single-Hand Touch) | **Web** for high-volume desk work; **Mobile** for roaming triage |
| **Offline Resilience** | ⚠️ Online-only (requires active internet) | ⚡ **100% Offline-First** (Room DB + Outbox Sync) | **Mobile** operates seamlessly during clinic Wi-Fi drops |
| **Authentication Methods** | Email/Password, Google OAuth Redirect | Email/Password, Native SMS OTP, Google 1-Tap | **Mobile** provides frictionless 1-tap phone OTP (biometric sign-in is not yet implemented, despite the `androidx.biometric` dependency being present) |
| **OPD Queue View** | Multi-column table with filtering & polling | Real-time card feed with status badges & vitals | **Mobile** for quick glance & status; **Web** for batch oversight |
| **Queue Ordering** | Timestamp column sortable | ⏱️ **Strict FIFO Priority** + Completed deprioritized | **Mobile** ensures strict first-in, first-out patient fairness |
| **Walk-In Registration** | Comprehensive modal form | Fast mobile registration bottom sheet | **Both** register walk-ins directly into today's queue |
| **Vitals Recording** | Embedded in visit modal / consultation | 🩺 **Instant Dialog** (BP, pulse, SpO2, temp, weight, height — plain numeric fields; 1-tap presets are not yet implemented) | **Mobile** for rapid bedside / triage queue vitals capture |
| **Clinical Severity Alerts** | Text warnings & severity banners | High-visibility color badges & alert banners | **Both** highlight abnormal BP, pulse, temp, or SpO2 |
| **Patient Directory Search** | Instant filter across name, phone, UHID | Real-time debounced search by name/phone/UHID | **Both** provide sub-second patient record lookup |
| **Patient Photo Capture** | File picker (PNG/JPEG upload) | ❌ Not yet implemented — the upload endpoint exists server-side but no camera/gallery flow calls it from mobile | **Web** exclusively, for now |
| **Document Scanning** | Drag-and-drop file uploader (PDF/Images) | ❌ Not yet implemented — the "Scan Lab" button currently shows a placeholder toast, no camera opens | **Web** exclusively, for now |
| **Appointment Management** | Full multi-column day/week/month calendar | Horizontal date strip + status filtered tabs | **Web** for complex scheduling; **Mobile** for quick day check-in |
| **Appointment Check-In** | 1-Click "Check-In" to OPD Queue | 1-Tap "Check In" button converting to Queue entry | **Both** immediately transition appointments into active queue |
| **Billing & Payments** | Multi-line item invoice builder, ledger, tax | Streamlined mobile bill creation & quick collect | **Web** for complex itemized invoices; **Mobile** for quick fees |
| **Payment Collection** | Cash, UPI, Card, Net Banking, Insurance | 1-Tap Cash, UPI, Card, Insurance modal | **Both** record payments and update invoice status |
| **Receipt Generation** | Browser print dialog & formatted PDF | Digital receipt view & instant sharing | **Web** for physical thermal/A4 printing; **Mobile** for digital |
| **Self-Check-In Kiosk Triage** | Full table view with doctor assignment | Swipeable card list with 1-tap doctor assignment | **Both** convert kiosk walk-ins into active doctor queues |
| **Patient Direct Communication** | Web WhatsApp link via phone URL | 📞 **1-Tap Direct Phone Call & Native WhatsApp** | **Mobile** enables instant phone dialer and WhatsApp chat |
| **Staff & Clinic Administration** | Manage staff profiles, roles, fees, rosters | Read-only doctor list & active clinic indicator | **Web** exclusively for practice admin operations |
| **Practice Analytics** | Footfall graphs, revenue trends, top diagnoses | Daily summary stat counters (Arrived, Waiting, Done) | **Web** for deep financial & operational insights |
| **AI Assistant / Copilot** | 🤖 Embedded `useChatAssistant` staff widget | 🤖 Shipped in v0.4.0 — chat with "Swati," which can propose (never silently execute) creating a patient, registering a queue entry, recording vitals, or booking an appointment, pending explicit staff confirmation | **Both**, gated per-clinic by an admin toggle |
| **Over-The-Air (OTA) Updates** | Instant web deployment (Amplify) | 🚀 **Firebase App Distribution** (`pilot-doctors`) | **Mobile** |

---

## 3. Role-by-Role Deep Dive

### A. Receptionist (Front Desk Operations)

#### 1. On Web (`medray-platform`)
* **Primary Responsibilities**: Front desk check-in, advance appointment booking, complex multi-item billing, receipt printing, and staff directory management.
* **Key Web Strengths**:
  - **Comprehensive Billing**: Create detailed invoices with custom line items, consultation charges, procedure fees, discounts, and taxes.
  - **Printing Hardware Integration**: Direct output to thermal receipt printers, laser printers, and PDF generation for printed invoices and prescription copies.
  - **Calendar Scheduling**: Full interactive calendar grid allowing staff to view all doctors' schedules side-by-side across weeks and months.
  - **Staff Management**: Receptionists can view staff rosters and update non-security staff attributes (specializations, experience).
  - **AI Copilot**: Floating conversational assistant (`useChatAssistant`) to look up clinic policies, pricing, and operational workflows.

#### 2. On Mobile (`medray-staff`)
* **Primary Responsibilities**: Roaming intake, fast walk-in registration, quick fee collection, kiosk check-in triage, and direct patient outreach.
* **Key Mobile Strengths**:
  - **Ultra-Fast Walk-In Entry**: Rapid 3-field modal (Patient Name, Phone, Doctor) adding patients to the active queue in under 10 seconds.
  - **Instant Fee Collection**: One-tap payment modal on every patient card to log Cash/UPI payments without navigating away from the queue.
  - **Direct Patient Calling**: One-tap phone dialer and WhatsApp intent to call patients waiting outside or in the lounge.
  - *(Patient-photo camera capture during registration is planned but not yet implemented — the upload endpoint exists, no UI calls it yet.)*

---

### B. Nurse (Triage & Nursing Station)

#### 1. On Web (`medray-platform`)
* **Primary Responsibilities**: Reviewing scheduled patients, checking medical records, uploading external laboratory reports, and checking in arrivals.
* **Key Web Strengths**:
  - **Document Archiving**: Drag-and-drop multi-page diagnostic PDFs, lab reports, and imaging files directly into patient health records.
  - **Longitudinal EHR Inspection**: View full historical visit timelines, previous doctor prescriptions, and allergy notes on a wide desktop layout.
  - **Role-Based Security**: Automatic restriction from financial billing tables and administrative audit logs.

#### 2. On Mobile (`medray-staff`)
* **Primary Responsibilities**: High-mobility triage, measuring and recording vital signs, and queuing patients for doctor examination.
* **Key Mobile Strengths**:
  - **Universal Fast Vitals Entry (`FastVitalsEntryDialog`)**:
    - Accessible directly from every patient card in the queue.
    - Fields: Blood Pressure ($mmHg$), Pulse ($bpm$), $\text{SpO}_2$ ($\%$) with pulse oximeter ranges, Temperature ($^\circ\text{F}$), Weight ($kg$), and Height ($cm$) — plain numeric entry (1-tap quick presets are planned but not yet implemented).
    - **Real-Time Severity Alerts**: Dynamic warning banners and color-coded badges for hypertensive crises, hypoxemia, or high fever.
    - **Live Doctor Sync**: Instantly pushes vitals to the Doctor Android Tablet app via `PATCH /queue/{id}/vitals`.
  - *(Point-and-shoot capture of paper prescriptions/lab slips is planned but not yet implemented — the "Scan Lab" button currently shows a placeholder message.)*
  - **Uninterrupted Offline Triage**: Vitals logging during Wi-Fi drops is automatically queued in Room DB and synced via Outbox when network reconnects.

---

## 4. Feature Domain Breakdown

### 4.1 OPD Queue & Live Triage
* **Web**:
  - Filterable table view by doctor, arrival status, and date.
  - Configurable polling intervals (`30s`, `1m`, `5m`) with manual refresh.
* **Mobile**:
  - Modern card stream modeled after the Doctor Tablet app with status pills (`Arrived`, `Waiting`, `In Triage`, `Completed`).
  - **Strict FIFO Sorting**:
    ```sql
    SELECT * FROM queue_entries WHERE clinicId = :clinicId 
    ORDER BY CASE WHEN status IN ('COMPLETED', 'CANCELLED', 'NO_SHOW') THEN 2 ELSE 1 END ASC,
    COALESCE(createdAt, scheduledAt) ASC, opdNumber ASC
    ```
  - Priority ordering ensures active patients are seen in arrival order, while completed/cancelled records automatically drop to the bottom.

### 4.2 Appointments & Scheduling
* **Web**:
  - Full calendar supporting month, week, and day grid views.
  - Granular slot configuration, recurring slots, and doctor availability calendars.
* **Mobile**:
  - Clean horizontal date carousel with instant date switching.
  - Filter tabs: `All`, `Scheduled`, `Checked In`, `Completed`, `Cancelled`.
  - Direct 1-tap "Check In" button transforming an appointment into a live OPD Queue token.

### 4.3 Billing & Payments
* **Web**:
  - Full invoice creation with line-item breakdowns (Consultation, Diagnostics, Pharmacy, Nursing).
  - Tax calculation (GST/VAT), custom discounts, and payment method allocation.
  - Browser print stylesheets for 80mm thermal receipts and standard A4 invoices.
* **Mobile**:
  - Simplified billing feed categorized by `Pending`, `Paid`, and `Partial`.
  - Fast Payment Collection sheet (Cash, UPI, Card, Insurance) with immediate status update.
  - Digital receipt inspection and shareable summaries.

### 4.4 Self Check-In Kiosks
* **Web & Mobile**:
  - Live feed of patients who checked in via the clinic entrance QR code / tablet kiosk.
  - Front desk or triage nurse can review patient details, select an available doctor, and assign the patient directly to the live OPD queue.

### 4.5 Offline Architecture & Data Synchronization
* **Web**:
  - Relies on constant internet connectivity; in-progress form drafts are backed up in `sessionStorage` / `localStorage`.
* **Mobile**:
  - **Offline-tolerant for queue/vitals/registration**: local Room DB (`queue_entries`, `patients`, `outbox_commands` — appointments and billing do not currently have local tables, so those actions require connectivity).
  - **Outbox Manager**: When offline, vitals updates, status updates, and walk-in registrations are stored locally and executed automatically with exponential backoff when connectivity resumes.

---

## 5. Summary Recommendations

| Use Case / Scenario | Recommended Surface | Why? |
|---|:---:|---|
| **Stationary Front Desk Check-In & Paper Receipt Printing** | 💻 **Web** | Best for high-speed typing, large monitors, and physical thermal printers. |
| **Bedside Vitals Recording & Triage Station** | 📱 **Mobile** | Lightweight, rapid 1-tap vitals presets, and immediate doctor tablet synchronization. |
| **Roaming Floor Registration / Queue Marshalling** | 📱 **Mobile** | Allows staff to register walk-ins and direct patients from anywhere in the waiting lounge. |
| **Paper Lab Report / Patient Photo Capture** | 💻 **Web** | Point-and-shoot mobile capture is planned but not yet implemented — use the web uploader today. |
| **Comprehensive Accounting, Invoicing & Roster Admin** | 💻 **Web** | Full itemized billing, financial reports, tax calculation, and staff role management. |
| **Unstable / Spotty Clinic Internet Connectivity** | 📱 **Mobile** | Local Room database and background Outbox synchronization prevent data loss. |
