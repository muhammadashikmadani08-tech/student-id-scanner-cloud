package com.institution.idscanner.cloud.util

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Institution logo storage — phone-only by design, like student photos.
 *
 * The picked image is copied into the app's internal storage under a
 * per-account file (keyed by the signed-in Firebase UID), so each Gmail
 * account has its own logo and the logo never leaves this phone. Only the
 * absolute file path is ever stored/returned — a raw content:// URI is
 * never persisted, because it stops working after a restart.
 *
 * The last signed-in UID is remembered in SharedPreferences so the
 * welcome screen can still show that account's logo before sign-in.
 */
object LogoStore {

    private const val PREFS_NAME = "institution_logo_prefs"
    private const val KEY_LAST_UID = "last_uid"

    private fun logoFile(context: Context, uid: String): File {
        val dir = File(context.filesDir, "logos")
        return File(dir, "$uid.jpg")
    }

    /** Absolute path of this account's saved logo, or null if none / file gone. */
    fun logoPath(context: Context, uid: String): String? {
        if (uid.isBlank()) return null
        val file = logoFile(context, uid)
        return if (file.exists()) file.absolutePath else null
    }

    /**
     * Copies the picked image into internal storage and returns its
     * absolute path, or null if the copy fails (previous logo kept).
     */
    fun saveLogo(context: Context, uid: String, uri: Uri): String? {
        if (uid.isBlank()) return null
        return try {
            val outFile = logoFile(context, uid)
            outFile.parentFile?.mkdirs()
            val input = context.contentResolver.openInputStream(uri) ?: return null
            input.use { src ->
                outFile.outputStream().use { dst -> src.copyTo(dst) }
            }
            setLastUid(context, uid)
            outFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    /** Deletes this account's saved logo; the default icon is used after. */
    fun clearLogo(context: Context, uid: String) {
        if (uid.isBlank()) return
        try {
            logoFile(context, uid).delete()
        } catch (_: Exception) {
            // A missing logo file must not block anything.
        }
    }

    fun setLastUid(context: Context, uid: String) {
        if (uid.isBlank()) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LAST_UID, uid).apply()
    }

    fun lastUid(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_UID, "").orEmpty()
    }
}
