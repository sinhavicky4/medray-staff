# MedRay AI — Staff App (Nurse / Receptionist): Feature Reference

Code-verified as of 2026-08-28, against `medray-staff/app/src/main/java/ai/medray/staff/`. For platform-wide architecture see `medray-platform/docs/SYSTEM_ARCHITECTURE_AS_BUILT.md`. For a stakeholder-facing comparison against the web app, see `docs/STAFF_FEATURES_WEB_VS_MOBILE.md` — but note the **Known Gaps** section below corrects several claims that document makes.

## 1. What This App Is

A native Android app, package `ai.medray.staff` (Kotlin 2.0, Jetpack Compose + Material3, `minSdk 26`), current version **0.5.0**. Built for **Nurses** (fast one-handed vitals triage) and **Receptionists** (walk-in registration, UPI billing, appointment check-in), offline-first. This is a real, actively-developed app (~7,300 lines of Kotlin, 32 commits, genuine production bug-fix history in `RELEASE_NOTES.md`) shipping via CI to a Firebase App Distribution pilot group — **not** on the Google Play Store.

## 2. Architecture

- **Organization**: feature-based packages under `ui/` (`auth`, `splash`, `nurse`, `reception`, `patients`, `appointments`, `billing`, `selfcheckins`, `profile`, `chat`, `common`, `navigation`, `theme`). Unlike the Doctor app, there are no per-screen ViewModel classes — all screen state lives in one composable state holder, `StaffAppNavHost` (`ui/navigation/StaffNavGraph.kt`, 1,322 lines), which owns every `remember { mutableStateOf(...) }`, wires 9 hand-constructed repositories to screens, and hosts the `NavHost` plus all modal dialogs.
- **Single Activity**: `MainActivity.kt` constructs all repositories by hand and passes them into `StaffAppNavHost`.
- **Local persistence**: Room `StaffDatabase` (v2) with 3 entities only — `patients`, `queue_entries`, `outbox_commands`. **Plain, unencrypted** Room (no SQLCipher — see Known Gaps).
- **Offline sync**: WorkManager `OutboxSyncWorker` with exponential backoff, replaying exactly 3 command types: `UPDATE_VITALS`, `UPDATE_STATUS`, `REGISTER_QUEUE`. Appointments, billing, and self-check-in assignment are **not** queued offline.
- **Networking**: Retrofit2 + Gson + OkHttp3 via a single `ApiClient` singleton, `CsrfInterceptor` + `SessionCookieJar` attached. Same production API base URL hardcoded for both debug and release build types — no separate staging endpoint.

## 3. Backend Integration & Auth

Calls the **same production API** as the web portal and Doctor tablet app (`StaffApiService.kt`, 30+ endpoints mapping directly onto `medray-platform/api/src/routes/*.ts`) — this is one shared backend, not a separate service.

Auth is **cookie/session-based**, matching the web app, with three login paths, all hitting real production auth routes:
- **Mobile OTP**: `POST auth/otp/request` → `POST auth/otp/verify`.
- **Email/password**: `POST auth/login`.
- **Google Sign-In**: Android Credential Manager → ID token → `POST auth/google/mobile`.

Session cookie + CSRF cookie stored in `EncryptedSharedPreferences` (AES-256), replayed via `SessionCookieJar`/`CsrfInterceptor`. Doctor accounts are explicitly blocked server-side from this app ("This account is not authorized for mobile staff triage"); `User.isNurse`/`isReceptionist` derived flags branch the Queue screen and bottom-nav between the two role UIs.

## 4. Feature List

### Auth & Onboarding
1. Animated splash screen with silent session check.
2. Login — phone/OTP, email/password, or "Continue with Google" (Credential Manager, with Play-Services-intent fallback), all in one screen.
3. OTP verification — 6-digit entry + resend.
4. Role-aware error messaging (blocks Doctor accounts; guides a no-Google-email user to OTP or their Clinic Admin).

### OPD Queue / Triage — role-branched (same route, different screen)
5. **Nurse queue** — card-based live queue, 2×2 tap-to-filter KPI stat cards (Arrived/Waiting/In Triage/Completed), search by name/phone/UHID, strict FIFO with completed/cancelled pushed to the bottom, "Record/Edit Vitals", "View Rx", "Scan Lab" (see Known Gaps), one-tap "Mark Arrived".
6. **Receptionist queue** — same card/KPI/FIFO pattern plus doctor filter, "+ Walk-In" quick action, "Collect Payment" (dynamic UPI QR) per card, one-tap WhatsApp messaging.
7. **Fast Vitals Entry** — BP/pulse/SpO2/temperature/weight/height, live BMI calc, real-time severity banner (hypertensive crisis, hypoxia, high/low fever, tachy/bradycardia) via `VitalsValidator`; syncs via `PATCH /queue/{id}/vitals`, read directly by the Doctor tablet app.
8. **Walk-In Register** — tabbed new-walk-in / existing-patient-lookup registration, atomically creates/finds the patient and adds them to a doctor's queue with a chief complaint, issues an OPD token.
9. **Add existing patient to queue** — same flow, launched from the Patients Directory.
10. **Prescription Viewer** — "View Rx" opens the patient's latest visit/prescription with a WhatsApp-share action.
11. Pull-to-refresh across Queue, Patients, and Billing.

### Patients Directory
12. Patients screen — KPI counters (Total, Male, Female, Seniors 60+), live debounced search, quick filter pills, "Register Patient" and "Add to Queue" entry points.
13. Patient Details — Overview / Visit history / Prescriptions tabs, "Add to Queue" action.

### Appointments
14. Appointments screen — date-strip navigation, status filter tabs, KPI cards, one-tap "Check In" (converts to a live OPD queue token) and "Cancel".

### Billing & Payments
15. Billing screen — revenue KPI counters (Total Collected / Pending Due), invoice list with status badges, search, "Collect Payment" (dynamic UPI QR).
16. Invoice Detail — line items, totals, payment history, Send via WhatsApp/Email, PDF download, print.
17. **Dynamic UPI QR** — live NPCI-compliant `upi://pay?...` QR scoped to the clinic's real configured VPA; refuses to render with a placeholder if the clinic hasn't configured one.
18. Pre-visit ("advance") fee collection from the Queue screen, before a doctor visit/invoice exists — reconciled into the real invoice later.

### Self Check-In Kiosk
19. Self Check-Ins feed — patients who checked in via the entrance kiosk/QR, pending-count badge in the drawer.
20. Assign Doctor — converts a kiosk check-in into a live queue entry.

### AI Chat Assistant ("Swati", new in v0.4.0)
21. Chat screen — suggested-prompt empty state, typing indicator, error banner. Gated server-side to clinics with `chatAssistantEnabled`.
22. **Propose → confirm action pattern** — the assistant can propose creating a patient, registering a queue entry, recording vitals, or booking an appointment; a `PendingActionCard` shows the draft, and only explicit staff confirmation triggers the actual write (via the same repository methods the manual flows use).

### Profile & Shell
23. Staff Profile — identity card, role badge, assigned clinic details incl. UPI VPA, confirm-before sign-out.
24. Navigation drawer mirroring the web sidebar (OPD Queue, Patients, Appointments, Billing, Self Check-Ins, Profile, Chat if enabled, Sign Out).
25. Role-aware bottom nav — Nurses: Triage/Patients/Appointments/Profile. Receptionists: OPD Queue/Patients/Billing/Check-Ins.

## 5. Build & Deploy

- **CI** (`.github/workflows/android-ci.yml`): `./gradlew testDebugUnitTest assembleDebug` on push/PR to `master`/`develop`.
- **Release** (`.github/workflows/firebase-distribution.yml`): triggered by a `v*` tag or manual dispatch; signs and uploads to Firebase App Distribution, target group defaults to `pilot-doctors` (shared group name with the Doctor app — worth confirming who's actually in it), also creates a GitHub Release.
- Not on Google Play — pilot/pre-production distribution only. Firebase project is real and configured (Crashlytics + Performance + App Distribution all wired).

## 6. Known Gaps — Not Yet Implemented

These directly contradict claims in `docs/STAFF_FEATURES_WEB_VS_MOBILE.md` and `README.md` — treat those documents' claims with caution until updated:

1. **CameraX document scanning — not implemented at all.** CameraX is a declared Gradle dependency and the manifest requests `CAMERA` permission, but there is zero CameraX code anywhere. The "Scan Lab" button and `onScanDocumentClick` just show `Toast.makeText(context, "Document Scanner opened for ...")` — no camera ever opens.
2. **Patient photo capture — not implemented.** No camera-intent/CameraX code in registration/patient screens. The backend endpoint (`uploadPatientPhoto`) exists but is never called from any UI.
3. **Document upload is wired server-side but completely unreachable.** `DocumentRepository` exists but is never instantiated in `MainActivity.kt` or passed to `StaffAppNavHost` — dead code.
4. **Biometric sign-in — not implemented.** `androidx.biometric` is a declared dependency with `USE_BIOMETRIC` permission, but there is no `BiometricPrompt`/`BiometricManager` usage anywhere.
5. **Vitals "1-Tap Quick Presets" (⚡ Normal / 🌡️ Fever / ⚠️ High BP) — not implemented.** `RELEASE_NOTES.md` v0.1.1 claims these; `FastVitalsEntryDialog` has plain numeric fields only, no preset buttons.
6. **"Room SQLite + SQLCipher encryption" — false.** No SQLCipher dependency anywhere; `StaffDatabase.kt` builds a plain, unencrypted `Room.databaseBuilder`. (EncryptedSharedPreferences for cookies/tokens is genuinely real — that's a separate mechanism.)
7. **Offline outbox covers 3 command types, not "queue_entries, patients, appointments, bills, and outbox_commands."** Only `patients`, `queue_entries`, `outbox_commands` exist locally; appointments/billing/self-check-in assignment are never queued offline.
8. **`AuthInterceptor.kt` (Bearer-token auth) is dead code** — fully implemented but never added to the `OkHttpClient`; real auth is entirely cookie-based via `SessionCookieJar`/`CsrfInterceptor`. Looks like a superseded earlier approach, not deleted.
9. **`createInvoice` endpoint intentionally points at a non-existent server route** (explicit code comment) — manual invoice creation from the app is not possible; invoices only auto-generate when a doctor completes a visit. Not called from any UI today, consistent with this.

None of the above affect the genuinely shipped, end-to-end-wired features: Queue, vitals entry (sans presets), walk-in registration, appointments, billing/UPI collection, self-check-in assign, patients directory, and the chat assistant.
