package ai.medray.staff.domain

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import ai.medray.staff.data.model.Invoice
import ai.medray.staff.data.model.formatIsoDateTimeLocal

/**
 * Renders an [Invoice] to a single-page A4 [PdfDocument] — the mobile
 * equivalent of the web app's browser-native window.print() on
 * InvoiceDetailClient.tsx. Kept intentionally simple (plain text rows, no
 * server round-trip) since this only needs to cover reception handing a
 * patient a paper/PDF copy, not a pixel-perfect match of the web layout.
 */
object InvoicePdfGenerator {
    private const val PAGE_WIDTH = 595 // A4 at 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    fun generate(invoice: Invoice, clinicName: String): PdfDocument {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true }
        val headingPaint = Paint().apply { textSize = 13f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 11f }
        val labelPaint = Paint().apply { textSize = 10f; color = 0xFF64748B.toInt() }
        val linePaint = Paint().apply { color = 0xFFCBD5E1.toInt(); strokeWidth = 1f }

        var y = MARGIN + 10f
        canvas.drawText(clinicName, MARGIN, y, titlePaint)
        y += 22f
        canvas.drawText("INVOICE", MARGIN, y, headingPaint)

        val rightX = PAGE_WIDTH - MARGIN
        canvas.drawText("INV-${invoice.invoiceNumber}", rightX - bodyPaint.measureText("INV-${invoice.invoiceNumber}"), MARGIN + 10f, headingPaint)
        canvas.drawText(
            invoice.status.name,
            rightX - bodyPaint.measureText(invoice.status.name),
            MARGIN + 30f,
            labelPaint,
        )
        if (!invoice.createdAt.isNullOrBlank()) {
            val dateText = formatIsoDateTimeLocal(invoice.createdAt)
            canvas.drawText(dateText, rightX - bodyPaint.measureText(dateText), MARGIN + 46f, labelPaint)
        }

        y += 20f
        canvas.drawLine(MARGIN, y, rightX, y, linePaint)
        y += 24f

        val patient = invoice.patient
        canvas.drawText(patient?.fullName ?: "OPD Patient", MARGIN, y, headingPaint)
        y += 16f
        val patientMeta = listOfNotNull(
            patient?.uhid?.ifBlank { null }?.let { "UHID $it" },
            patient?.phone?.ifBlank { null },
        ).joinToString("   ·   ")
        if (patientMeta.isNotBlank()) {
            canvas.drawText(patientMeta, MARGIN, y, labelPaint)
            y += 24f
        } else {
            y += 8f
        }

        canvas.drawLine(MARGIN, y, rightX, y, linePaint)
        y += 20f
        canvas.drawText("Description", MARGIN, y, labelPaint)
        canvas.drawText("Amount", rightX - bodyPaint.measureText("Amount"), y, labelPaint)
        y += 8f
        canvas.drawLine(MARGIN, y, rightX, y, linePaint)
        y += 20f

        invoice.lineItems.forEach { item ->
            canvas.drawText(item.description, MARGIN, y, bodyPaint)
            val amountText = "Rs. ${item.amount.toInt()}"
            canvas.drawText(amountText, rightX - bodyPaint.measureText(amountText), y, bodyPaint)
            y += 20f
        }

        y += 8f
        canvas.drawLine(MARGIN, y, rightX, y, linePaint)
        y += 24f

        fun totalRow(label: String, amount: Double, bold: Boolean = false) {
            val paint = if (bold) headingPaint else bodyPaint
            canvas.drawText(label, MARGIN, y, if (bold) headingPaint else labelPaint)
            val amountText = "Rs. ${amount.toInt()}"
            canvas.drawText(amountText, rightX - paint.measureText(amountText), y, paint)
            y += 20f
        }

        totalRow("Subtotal", invoice.subtotal)
        if (invoice.discountAmount > 0) totalRow("Discount", -invoice.discountAmount)
        totalRow("Total", invoice.total, bold = true)
        totalRow("Paid", invoice.netPaid)
        totalRow("Balance Due", invoice.balanceDue, bold = true)

        if (invoice.payments.isNotEmpty()) {
            y += 12f
            canvas.drawLine(MARGIN, y, rightX, y, linePaint)
            y += 20f
            canvas.drawText("Payments", MARGIN, y, headingPaint)
            y += 18f
            invoice.payments.forEach { payment ->
                val line = "${formatIsoDateTimeLocal(payment.recordedAt)}   ${payment.method.name}"
                canvas.drawText(line, MARGIN, y, bodyPaint)
                val amountText = "Rs. ${payment.amount.toInt()}"
                canvas.drawText(amountText, rightX - bodyPaint.measureText(amountText), y, bodyPaint)
                y += 18f
            }
        }

        canvas.drawText(
            "This is a computer-generated invoice.",
            MARGIN,
            PAGE_HEIGHT - MARGIN,
            labelPaint,
        )

        document.finishPage(page)
        return document
    }
}
