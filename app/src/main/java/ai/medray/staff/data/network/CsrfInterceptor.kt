package ai.medray.staff.data.network

import okhttp3.Interceptor
import okhttp3.Response

private val SAFE_METHODS = setOf("GET", "HEAD", "OPTIONS")

class CsrfInterceptor(private val cookieJar: SessionCookieJar) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder()

        // Clinic scope
        val clinicId = cookieJar.getActiveClinicId()
        if (!clinicId.isNullOrBlank()) {
            builder.header("X-Clinic-Id", clinicId)
        }

        builder.header("Accept", "application/json")
        builder.header("User-Agent", "MedRay-Staff-Android/0.1")

        if (request.method !in SAFE_METHODS) {
            val token = cookieJar.csrfToken()
            if (!token.isNullOrBlank()) {
                builder.header("X-CSRF-Token", token)
            }
        }

        return chain.proceed(builder.build())
    }
}
