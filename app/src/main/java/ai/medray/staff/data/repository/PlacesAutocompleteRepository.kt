package ai.medray.staff.data.repository

import android.content.Context
import ai.medray.staff.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** One autocomplete row — split into primary/secondary the way Google's own UI does (e.g. "MG Road" / "Bengaluru, Karnataka, India"). */
data class AddressSuggestion(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String
)

/**
 * Address autocomplete for the "Sign Up Your Clinic" form's optional
 * Address field — Android counterpart to web's AddressAutocompleteInput.tsx
 * (same India-only restriction, same predict-then-fetch-formatted-address
 * shape).
 *
 * Talks to Places API (New) — `places:autocomplete` and `GET places/{id}`
 * — directly over plain OkHttp rather than through
 * com.google.android.libraries.places's PlacesClient. That client's
 * findAutocompletePredictions()/fetchPlace() route to the *legacy* Places
 * API under the hood, and this GCP project — the same one web uses — only
 * has Places API (New) enabled (confirmed via `ApiException: 9011 "You're
 * calling a legacy API, which is not enabled for your project"`). That's
 * the exact constraint web's AddressAutocompleteInput.tsx comment already
 * describes for the JS `google.maps.places.Autocomplete` widget — Places
 * API (New) has no equivalent Android *client* SDK, only a REST surface,
 * so this calls it the same way any other backend integration in this app
 * would: a plain HTTP request.
 *
 * Every method degrades to an empty/failed Result rather than throwing
 * when `PLACES_API_KEY` is blank, so a build without a key configured
 * still has a fully usable plain text field, just without suggestions.
 */
class PlacesAutocompleteRepository(@Suppress("UNUSED_PARAMETER") context: Context) {
    private val apiKey = BuildConfig.PLACES_API_KEY
    private val http = OkHttpClient()
    private val jsonMediaType = "application/json".toMediaType()

    // One token per search session (first keystroke -> a place is fetched),
    // matching Google's billing model — reused across predict() calls,
    // discarded once fetchFormattedAddress() completes a selection.
    private var sessionToken: String? = null

    suspend fun predict(query: String): Result<List<AddressSuggestion>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || query.isBlank()) return@withContext Result.success(emptyList())
        val token = sessionToken ?: UUID.randomUUID().toString().also { sessionToken = it }
        val requestBody = JSONObject().apply {
            put("input", query)
            put("includedRegionCodes", JSONArray(listOf("in")))
            put("sessionToken", token)
        }
        val request = Request.Builder()
            .url("https://places.googleapis.com/v1/places:autocomplete")
            .addHeader("X-Goog-Api-Key", apiKey)
            .post(requestBody.toString().toRequestBody(jsonMediaType))
            .build()
        try {
            http.newCall(request).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Result.failure(Exception("Places autocomplete failed (${response.code}): $bodyText"))
                } else {
                    val suggestions = JSONObject(bodyText).optJSONArray("suggestions") ?: JSONArray()
                    Result.success(
                        (0 until suggestions.length()).mapNotNull { i ->
                            val prediction = suggestions.getJSONObject(i).optJSONObject("placePrediction") ?: return@mapNotNull null
                            val structured = prediction.optJSONObject("structuredFormat")
                            val primaryText = structured?.optJSONObject("mainText")?.optString("text")
                                ?: prediction.optJSONObject("text")?.optString("text").orEmpty()
                            val secondaryText = structured?.optJSONObject("secondaryText")?.optString("text").orEmpty()
                            AddressSuggestion(
                                placeId = prediction.optString("placeId"),
                                primaryText = primaryText,
                                secondaryText = secondaryText
                            )
                        }
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchFormattedAddress(placeId: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(IllegalStateException("Places API key not configured"))
        val token = sessionToken
        // The session ends with this fetch either way — a new query after
        // picking a place starts a fresh (separately billed) session.
        sessionToken = null
        val url = "https://places.googleapis.com/v1/places/$placeId" + if (token != null) "?sessionToken=$token" else ""
        val request = Request.Builder()
            .url(url)
            .addHeader("X-Goog-Api-Key", apiKey)
            .addHeader("X-Goog-FieldMask", "formattedAddress")
            .get()
            .build()
        try {
            http.newCall(request).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Result.failure(Exception("Places details failed (${response.code}): $bodyText"))
                } else {
                    Result.success(JSONObject(bodyText).optString("formattedAddress", ""))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
