package ai.medray.staff.domain

import android.content.ContentValues
import android.content.Context
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import ai.medray.staff.data.model.Invoice
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * "Download" and "Print" actions for [InvoicePdfGenerator]'s output — the
 * mobile equivalent of the web app's "Print / Save as PDF" button. Download
 * only supports API 29+ (MediaStore.Downloads, no storage permission
 * needed); pre-29 devices are out of scope for this pilot rollout — Print
 * still works there since android.print predates scoped storage entirely.
 */
object InvoicePdfActions {

    /** Saves the invoice as a PDF into the device's real Downloads folder. Returns null on failure. */
    fun downloadToDownloads(context: Context, invoice: Invoice, clinicName: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val fileName = "Invoice-${invoice.invoiceNumber}.pdf"
        val document = InvoicePdfGenerator.generate(invoice, clinicName)
        return try {
            val values = ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(android.provider.MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return null
            resolver.openOutputStream(uri)?.use { out: OutputStream -> document.writeTo(out) }
            fileName
        } catch (_: Exception) {
            null
        } finally {
            document.close()
        }
    }

    /** Opens Android's native print dialog (which itself offers "Save as PDF" as a destination). */
    fun print(context: Context, invoice: Invoice, clinicName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "Invoice-${invoice.invoiceNumber}"

        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: android.os.Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out android.print.PageRange>?,
                destination: android.os.ParcelFileDescriptor?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                val document: PdfDocument = InvoicePdfGenerator.generate(invoice, clinicName)
                try {
                    destination?.let { fd ->
                        FileOutputStream(fd.fileDescriptor).use { out -> document.writeTo(out) }
                    }
                    callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                } finally {
                    document.close()
                }
            }
        }

        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }
}
