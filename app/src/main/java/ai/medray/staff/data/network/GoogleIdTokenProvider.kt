package ai.medray.staff.data.network

import ai.medray.staff.BuildConfig
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

class GoogleIdTokenProvider {

    suspend fun requestIdToken(activityContext: Context): Result<String> {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val result = credentialManager.getCredential(activityContext, request)
            val googleCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            Result.success(googleCredential.idToken)
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Sign-in cancelled."))
        } catch (e: NoCredentialException) {
            Result.failure(Exception("No Google account found on this device. Add one in Settings, or sign in with your mobile number."))
        } catch (e: GetCredentialException) {
            Result.failure(Exception(e.message ?: "Google sign-in failed. Please try again or use your mobile number."))
        } catch (e: GoogleIdTokenParsingException) {
            Result.failure(Exception("Google sign-in parsing failed."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
