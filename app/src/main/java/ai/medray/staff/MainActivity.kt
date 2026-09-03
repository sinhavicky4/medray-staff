package ai.medray.staff

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.android.libraries.places.api.Places
import ai.medray.staff.data.repository.*
import ai.medray.staff.ui.navigation.StaffAppNavHost
import ai.medray.staff.ui.theme.MedRayStaffTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Address autocomplete on the clinic sign-up form needs this before
        // any PlacesClient is created — no-ops (leaves the client unusable,
        // PlacesAutocompleteRepository catches and no-ops on that) when
        // PLACES_API_KEY wasn't set in local.properties.
        if (!Places.isInitialized() && BuildConfig.PLACES_API_KEY.isNotBlank()) {
            Places.initialize(applicationContext, BuildConfig.PLACES_API_KEY)
        }

        val authRepo = AuthRepository(this)
        val queueRepo = QueueRepository(this)
        val patientRepo = PatientRepository(this)
        val appointmentRepo = AppointmentRepository(this)
        val billingRepo = BillingRepository(this)
        val doctorRepo = DoctorRepository(this)
        val selfCheckInRepo = SelfCheckInRepository(this)
        val visitRepo = VisitRepository(this)
        val chatRepo = ChatRepository(this)
        val clinicSignupRepo = ClinicSignupRepository(this)
        val staffManagementRepo = StaffManagementRepository(this)
        val placesRepository = PlacesAutocompleteRepository(this)

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
                        appointmentRepo = appointmentRepo,
                        billingRepo = billingRepo,
                        doctorRepo = doctorRepo,
                        selfCheckInRepo = selfCheckInRepo,
                        visitRepo = visitRepo,
                        chatRepo = chatRepo,
                        clinicSignupRepo = clinicSignupRepo,
                        staffManagementRepo = staffManagementRepo,
                        placesRepository = placesRepository
                    )
                }
            }
        }
    }
}
