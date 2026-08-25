# ============================================================================
# MedRay AI Android — Proguard & R8 Optimization / Obfuscation Rules
# ============================================================================

-dontoptimize
-dontobfuscate

# --- 1. Line Numbers & Stacktrace Deobfuscation (Crashlytics Mapping) ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions

# --- 2. Kotlin & Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# --- 3. Retrofit2 & OkHttp3 ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keepattributes EnclosingMethod

# --- 4. Gson & JSON DTO Serialization ---
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}
# Keep all MedRay Data Transfer Objects (DTOs) and Domain Models from being stripped
-keep class ai.medray.app.feature.**.data.remote.** { *; }
-keep class ai.medray.app.feature.**.domain.model.** { *; }
-keep class ai.medray.app.feature.**.data.local.** { *; }
-keep class ai.medray.app.core.config.** { *; }

# --- 5. Room Database & SQLite ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class androidx.room.RoomDatabase$Callback { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}

# --- 6. Dagger Hilt & Dependency Injection ---
-keep class * extends androidx.lifecycle.ViewModel
-keep class * extends androidx.hilt.work.HiltWorker
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-dontwarn dagger.hilt.**

# --- 7. AndroidX WorkManager ---
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-dontwarn androidx.work.**

# --- 8. AndroidX Security & Crypto ---
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# --- 9. AndroidX Biometric ---
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# --- 10. Coil Image Loader ---
-keep class coil.** { *; }
-dontwarn coil.**

# --- 11. Google Play Services / Credential Manager ---
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn androidx.credentials.**
