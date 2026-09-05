package ai.medray.staff.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

data class SelectedFile(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val bytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SelectedFile
        return uri == other.uri && name == other.name && sizeBytes == other.sizeBytes && mimeType == other.mimeType
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + sizeBytes.hashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}

object FileUtils {

    /**
     * Resolves display name and size from content URI using ContentResolver.
     */
    fun getFileMetadata(context: Context, uri: Uri): Pair<String, Long> {
        var name = "document_${System.currentTimeMillis()}"
        var size = 0L
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            name = cursor.getString(nameIndex) ?: name
                        }
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1) {
                            size = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } catch (_: Exception) {
            }
        } else if (uri.scheme == "file") {
            uri.path?.let { path ->
                val f = File(path)
                name = f.name
                size = f.length()
            }
        }
        return Pair(name, size)
    }

    /**
     * Reads all bytes from Uri with optional downsampling for huge images.
     */
    suspend fun readSelectedFile(context: Context, uri: Uri): SelectedFile? = withContext(Dispatchers.IO) {
        try {
            val (name, queriedSize) = getFileMetadata(context, uri)
            var mimeType = context.contentResolver.getType(uri) ?: when {
                name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                name.endsWith(".png", ignoreCase = true) -> "image/png"
                name.endsWith(".webp", ignoreCase = true) -> "image/webp"
                else -> "image/jpeg"
            }

            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null

            // If it's an image and exceeds 2MB, downsample it to keep network upload fast and reliable
            val finalBytes = if (mimeType.startsWith("image/") && rawBytes.size > 2 * 1024 * 1024) {
                downsampleImageBytes(rawBytes) ?: rawBytes
            } else {
                rawBytes
            }

            SelectedFile(
                uri = uri,
                name = name,
                sizeBytes = if (finalBytes.size != rawBytes.size) finalBytes.size.toLong() else if (queriedSize > 0) queriedSize else finalBytes.size.toLong(),
                mimeType = mimeType,
                bytes = finalBytes
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Downsample large bitmap and compress to JPEG at 85% quality.
     */
    private fun downsampleImageBytes(bytes: ByteArray, maxDimension: Int = 2048): ByteArray? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            var inSampleSize = 1
            while (options.outWidth / (inSampleSize * 2) >= maxDimension &&
                options.outHeight / (inSampleSize * 2) >= maxDimension
            ) {
                inSampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts) ?: return null
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            bitmap.recycle()
            out.toByteArray()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Creates a temporary file in the app cache and returns a FileProvider Uri for camera capture.
     */
    fun createCameraCaptureUri(context: Context): Uri {
        val captureDir = File(context.cacheDir, "captures").apply { mkdirs() }
        val captureFile = File(captureDir, "capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            captureFile
        )
    }

    /**
     * Formats bytes to human-readable string: e.g. "450 KB", "1.8 MB".
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        if (bytes < 1024) return "$bytes B"
        if (bytes < 1024 * 1024) return "${(bytes / 1024.0).toInt()} KB"
        return String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    }

    /**
     * Checks if mime type is allowed by backend (PDF, JPEG, PNG, WebP).
     */
    fun isSupportedMimeType(mimeType: String?): Boolean {
        if (mimeType == null) return false
        val normalized = mimeType.lowercase()
        return normalized == "application/pdf" ||
                normalized == "image/jpeg" ||
                normalized == "image/jpg" ||
                normalized == "image/png" ||
                normalized == "image/webp"
    }
}
