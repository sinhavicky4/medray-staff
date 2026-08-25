package ai.medray.staff

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance

class MedRayStaffApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        try {
            FirebaseApp.initializeApp(this)
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
            FirebasePerformance.getInstance().isPerformanceCollectionEnabled = !BuildConfig.DEBUG

            if (!BuildConfig.DEBUG) {
                FirebaseAppDistribution.getInstance().updateIfNewReleaseAvailable()
            }
        } catch (_: Exception) {
            // Firebase setup optional in pure offline dev
        }
    }
}
