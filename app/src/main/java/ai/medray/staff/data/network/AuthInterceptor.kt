package ai.medray.staff.data.network

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "medray_staff_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(token: String) {
        prefs.edit().putString("auth_token", token).apply()
    }

    fun getToken(): String? = prefs.getString("auth_token", null)

    fun clearToken() {
        prefs.edit().remove("auth_token").remove("active_clinic_id").apply()
    }

    fun saveActiveClinicId(clinicId: String) {
        prefs.edit().putString("active_clinic_id", clinicId).apply()
    }

    fun getActiveClinicId(): String? = prefs.getString("active_clinic_id", null)

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()

        val token = getToken()
        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
        }

        val clinicId = getActiveClinicId()
        if (!clinicId.isNullOrBlank()) {
            builder.header("X-Clinic-Id", clinicId)
        }

        builder.header("Accept", "application/json")
        builder.header("User-Agent", "MedRay-Staff-Android/0.1")

        return chain.proceed(builder.build())
    }
}
