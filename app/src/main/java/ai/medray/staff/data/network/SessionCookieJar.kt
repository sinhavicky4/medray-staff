package ai.medray.staff.data.network

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

private const val PREFS_NAME = "medray_staff_session_cookies"
private const val KEY_SESSION = "medray_session"
private const val KEY_CSRF = "medray_csrf"
private const val KEY_CLINIC_ID = "medray_clinic_id"

class SessionCookieJar(context: Context) : CookieJar {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val editor = prefs.edit()
        var changed = false
        cookies.forEach { cookie ->
            if (cookie.name == KEY_SESSION || cookie.name == KEY_CSRF) {
                editor.putString(cookie.name, cookie.value)
                changed = true
            }
        }
        if (changed) editor.apply()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookies = mutableListOf<Cookie>()
        prefs.getString(KEY_SESSION, null)?.let { cookies += buildCookie(url, KEY_SESSION, it) }
        prefs.getString(KEY_CSRF, null)?.let { cookies += buildCookie(url, KEY_CSRF, it) }
        return cookies
    }

    fun csrfToken(): String? = prefs.getString(KEY_CSRF, null)

    fun hasSession(): Boolean = prefs.getString(KEY_SESSION, null) != null

    fun getActiveClinicId(): String? = prefs.getString(KEY_CLINIC_ID, null)

    fun setActiveClinicId(clinicId: String) {
        prefs.edit().putString(KEY_CLINIC_ID, clinicId).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun buildCookie(url: HttpUrl, name: String, value: String): Cookie =
        Cookie.Builder()
            .domain(url.host)
            .path("/")
            .name(name)
            .value(value)
            .build()
}
