package ai.medray.staff.data.local

import android.content.Context

private const val PREFS_NAME = "medray_staff_app_prefs"
private const val KEY_SEEN_PERMISSIONS_PRIMER = "seen_permissions_primer"

/**
 * Small non-sensitive app-level flags — deliberately its own plain
 * (unencrypted) SharedPreferences file, separate from SessionCookieJar's
 * EncryptedSharedPreferences (data/network/SessionCookieJar.kt), since
 * nothing stored here is sensitive.
 *
 * Tracks whether the user has already been through PermissionsPrimerScreen
 * (StaffNavGraph.kt) so it's shown exactly once — on the very first
 * successful login on this device, never again after, regardless of
 * whether the permissions were actually granted or denied there.
 */
object AppPreferences {
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasSeenPermissionsPrimer(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SEEN_PERMISSIONS_PRIMER, false)

    fun setSeenPermissionsPrimer(context: Context) {
        prefs(context).edit().putBoolean(KEY_SEEN_PERMISSIONS_PRIMER, true).apply()
    }
}
