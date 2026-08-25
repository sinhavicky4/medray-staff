package ai.medray.staff.data.network

import android.content.Context
import ai.medray.staff.BuildConfig
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private var apiService: StaffApiService? = null
    private var cookieJar: SessionCookieJar? = null

    fun getCookieJar(context: Context): SessionCookieJar {
        return cookieJar ?: synchronized(this) {
            cookieJar ?: SessionCookieJar(context.applicationContext).also { cookieJar = it }
        }
    }

    fun getService(context: Context): StaffApiService {
        return apiService ?: synchronized(this) {
            apiService ?: buildService(context).also { apiService = it }
        }
    }

    private fun buildService(context: Context): StaffApiService {
        val jar = getCookieJar(context)
        val csrf = CsrfInterceptor(jar)

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        val okHttpClient = OkHttpClient.Builder()
            .cookieJar(jar)
            .addInterceptor(csrf)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val gson = GsonBuilder()
            .setLenient()
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        return retrofit.create(StaffApiService::class.java)
    }
}
