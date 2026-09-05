package ai.medray.staff.domain

import ai.medray.staff.core.util.FileUtils
import ai.medray.staff.data.model.DocumentKind
import ai.medray.staff.data.model.PatientDocument
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatientDocumentTest {

    private val gson = Gson()

    @Test
    fun testPatientDocumentDeserializationFromBackendResponse() {
        val json = """
            {
                "id": "doc-123",
                "patientId": "pat-456",
                "clinicId": "clinic-789",
                "visitId": "visit-001",
                "kind": "REPORT",
                "url": "https://s3.amazonaws.com/medray-bucket/doc-123.pdf?signature=xyz",
                "fileName": "blood_test_report.pdf",
                "sizeBytes": 2048576,
                "mimeType": "application/pdf",
                "createdAt": "2026-09-05T12:00:00Z",
                "uploadedBy": {
                    "id": "user-1",
                    "fullName": "Nurse Jenny"
                }
            }
        """.trimIndent()

        val doc = gson.fromJson(json, PatientDocument::class.java)

        assertEquals("doc-123", doc.id)
        assertEquals("pat-456", doc.patientId)
        assertEquals("REPORT", doc.kind)
        assertEquals("blood_test_report.pdf", doc.fileName)
        assertEquals(2048576L, doc.sizeBytes)
        assertEquals(2048576L, doc.displaySize)
        assertEquals("https://s3.amazonaws.com/medray-bucket/doc-123.pdf?signature=xyz", doc.displayUrl)
        assertEquals("Nurse Jenny", doc.uploadedBy?.fullName)
        assertEquals("REPORT", doc.effectiveKind)
    }

    @Test
    fun testPatientDocumentDeserializationLegacyFileUrlFallback() {
        val json = """
            {
                "id": "doc-legacy",
                "patientId": "pat-456",
                "clinicId": "clinic-789",
                "fileUrl": "https://cdn.medray.ai/legacy.jpg",
                "fileName": "rx.jpg",
                "fileSize": 102400,
                "createdAt": "2026-09-01T10:00:00Z"
            }
        """.trimIndent()

        val doc = gson.fromJson(json, PatientDocument::class.java)

        assertEquals("doc-legacy", doc.id)
        assertEquals("https://cdn.medray.ai/legacy.jpg", doc.displayUrl)
        assertEquals(102400L, doc.displaySize)
    }

    @Test
    fun testDocumentKindServerMapping() {
        assertEquals("REPORT", DocumentKind.REPORT.serverKind)
        assertEquals("REPORT", DocumentKind.LAB_REPORT.serverKind)
        assertEquals("REPORT", DocumentKind.RADIOLOGY.serverKind)
        assertEquals("PRESCRIPTION", DocumentKind.PRESCRIPTION.serverKind)
        assertEquals("GENERAL", DocumentKind.GENERAL.serverKind)
        assertEquals("GENERAL", DocumentKind.DISCHARGE_SUMMARY.serverKind)
        assertEquals("GENERAL", DocumentKind.INSURANCE.serverKind)
        assertEquals("GENERAL", DocumentKind.OTHER.serverKind)
    }

    @Test
    fun testFileUtilsFormatSize() {
        assertEquals("0 B", FileUtils.formatFileSize(0))
        assertEquals("512 B", FileUtils.formatFileSize(512))
        assertEquals("100 KB", FileUtils.formatFileSize(100 * 1024))
        assertEquals("1.5 MB", FileUtils.formatFileSize((1.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun testFileUtilsSupportedMimeTypes() {
        assertTrue(FileUtils.isSupportedMimeType("application/pdf"))
        assertTrue(FileUtils.isSupportedMimeType("image/jpeg"))
        assertTrue(FileUtils.isSupportedMimeType("image/jpg"))
        assertTrue(FileUtils.isSupportedMimeType("image/png"))
        assertTrue(FileUtils.isSupportedMimeType("image/webp"))

        assertFalse(FileUtils.isSupportedMimeType("video/mp4"))
        assertFalse(FileUtils.isSupportedMimeType("application/zip"))
        assertFalse(FileUtils.isSupportedMimeType(null))
    }
}
