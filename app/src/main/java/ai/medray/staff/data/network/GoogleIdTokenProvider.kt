package ai.medray.staff.data.network

import ai.medray.staff.BuildConfig
import android.content.Context
import android.content.Intent
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
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
            Result.failure(Exception("No Google credential found via Credential Manager."))
        } catch (e: GetCredentialException) {
            Result.failure(Exception(e.message ?: "Google Credential Manager failed."))
        } catch (e: GoogleIdTokenParsingException) {
            Result.failure(Exception("Google sign-in parsing failed."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getSignInIntent(context: Context): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    fun parseSignInResult(data: Intent?): Result<String> {
        if (data == null) return Result.failure(Exception("Sign-in cancelled."))
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        return try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (!idToken.isNullOrBlank()) {
                Result.success(idToken)
            } else {
                Result.failure(Exception("Google sign-in did not return an ID token."))
            }
        } catch (e: ApiException) {
            val msg = when (e.statusCode) {
                CommonStatusCodes.SIGN_IN_REQUIRED -> "Please sign into a Google account on this device."
                CommonStatusCodes.NETWORK_ERROR -> "Network error during Google sign-in. Check your connection."
                CommonStatusCodes.DEVELOPER_ERROR -> "Google sign-in configuration error (SHA-1 fingerprint not registered in Firebase/Google Cloud Console)."
                CommonStatusCodes.CANCELED -> "Google sign-in cancelled."
                else -> "Google sign-in error (Code: ${e.statusCode}): ${e.localizedMessage ?: "Unknown error"}"
            }
            Result.failure(Exception(msg))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
