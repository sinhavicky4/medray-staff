package ai.medray.staff

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import ai.medray.staff.data.repository.*
import ai.medray.staff.ui.navigation.StaffAppNavHost
import ai.medray.staff.ui.theme.MedRayStaffTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authRepo = AuthRepository(this)
        val queueRepo = QueueRepository(this)
        val patientRepo = PatientRepository(this)
        val billingRepo = BillingRepository(this)

        setContent {
            MedRayStaffTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StaffAppNavHost(
                        authRepo = authRepo,
                        queueRepo = queueRepo,
                        patientRepo = patientRepo,
                        billingRepo = billingRepo
                    )
                }
            }
        }
    }
}
