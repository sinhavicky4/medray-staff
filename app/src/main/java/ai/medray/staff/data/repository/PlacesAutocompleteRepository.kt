package ai.medray.staff.data.repository

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** One autocomplete row — split into primary/secondary the way Google's own UI does (e.g. "Dr Sharma Clinic" / "MG Road, Bengaluru"). */
data class AddressSuggestion(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String
)

/**
 * Address autocomplete for the "Sign Up Your Clinic" form's optional
 * Address field — Android counterpart to web's AddressAutocompleteInput.tsx
 * (same India-only restriction, same predict-then-fetch-formatted-address
 * shape). Needs `PLACES_API_KEY` set in local.properties and
 * `Places.initialize()` to have run (see MainActivity.onCreate) — every
 * method degrades to an empty/failed Result rather than throwing when that
 * hasn't happened, so a build without a key configured still has a fully
 * usable plain text field, just without suggestions.
 */
class PlacesAutocompleteRepository(context: Context) {
    private val client: PlacesClient? = if (Places.isInitialized()) Places.createClient(context) else null

    // One token per search session (first keystroke -> a place is fetched),
    // matching Google's billing model — reused across predict() calls,
    // discarded once fetchFormattedAddress() completes a selection.
    private var sessionToken: AutocompleteSessionToken? = null

    suspend fun predict(query: String): Result<List<AddressSuggestion>> {
        val places = client ?: return Result.success(emptyList())
        if (query.isBlank()) return Result.success(emptyList())
        val token = sessionToken ?: AutocompleteSessionToken.newInstance().also { sessionToken = it }
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setSessionToken(token)
            .setCountries(listOf("IN"))
            .build()
        return try {
            val response = places.findAutocompletePredictions(request).await()
            Result.success(
                response.autocompletePredictions.map {
                    AddressSuggestion(
                        placeId = it.placeId,
                        primaryText = it.getPrimaryText(null).toString(),
                        secondaryText = it.getSecondaryText(null).toString()
                    )
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchFormattedAddress(placeId: String): Result<String> {
        val places = client ?: return Result.failure(IllegalStateException("Places SDK not configured"))
        val builder = FetchPlaceRequest.builder(placeId, listOf(Place.Field.ADDRESS))
        sessionToken?.let { builder.setSessionToken(it) }
        // The session ends with this fetch either way — a new query after
        // picking a place starts a fresh (separately billed) session.
        sessionToken = null
        return try {
            val response = places.fetchPlace(builder.build()).await()
            Result.success(response.place.address ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
