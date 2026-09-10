package ai.medray.staff.domain

import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import ai.medray.staff.data.model.Invoice
import ai.medray.staff.data.model.formatIsoDateTimeLocal
import java.util.Locale

/**
 * Renders an [Invoice] to a single-page A4 [PdfDocument] formatted as a
 * standardized Indian healthcare "TAX INVOICE / BILL OF SUPPLY".
 * Compliant with GST Notification No. 12/2017-CT Rate (Exempt Healthcare Services).
 */
object InvoicePdfGenerator {
    private const val PAGE_WIDTH = 595 // A4 at 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f

    private val ONES = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
        "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    )
    private val TENS = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")

    private fun twoDigitWords(n: Int): String {
        if (n < 20) return ONES[n]
        return TENS[n / 10] + if (n % 10 != 0) " " + ONES[n % 10] else ""
    }

    private fun threeDigitWords(n: Int): String {
        val hundreds = n / 100
        val rest = n % 100
        return (if (hundreds > 0) "${ONES[hundreds]} Hundred" + (if (rest > 0) " " else "") else "") + (if (rest > 0) twoDigitWords(rest) else "")
    }

    fun amountInWords(amount: Double): String {
        val rupees = amount.toLong()
        var n = rupees
        val crore = (n / 10000000L).toInt()
        n %= 10000000L
        val lakh = (n / 100000L).toInt()
        n %= 100000L
        val thousand = (n / 1000L).toInt()
        n %= 1000L
        val hundred = n.toInt()

        val parts = mutableListOf<String>()
        if (crore > 0) parts.add("${threeDigitWords(crore)} Crore")
        if (lakh > 0) parts.add("${threeDigitWords(lakh)} Lakh")
        if (thousand > 0) parts.add("${threeDigitWords(thousand)} Thousand")
        if (hundred > 0) parts.add(threeDigitWords(hundred))

        val words = if (parts.isNotEmpty()) parts.joinToString(" ") else "Zero"
        return "Indian Rupees $words Only"
    }

    fun generate(invoice: Invoice, clinicName: String): PdfDocument {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val rightX = PAGE_WIDTH - MARGIN

        // Paints
        val clinicTitlePaint = Paint().apply { textSize = 16f; isFakeBoldText = true; color = 0xFF0F172A.toInt() }
        val clinicMetaPaint = Paint().apply { textSize = 8.5f; color = 0xFF475569.toInt() }
        val taxBadgePaint = Paint().apply { textSize = 8f; color = 0xFF334155.toInt() }
        val docTitlePaint = Paint().apply { textSize = 11f; isFakeBoldText = true; color = 0xFF0F172A.toInt() }
        val docSubPaint = Paint().apply { textSize = 7.5f; color = 0xFF64748B.toInt() }
        val statusPaint = Paint().apply { textSize = 8.5f; isFakeBoldText = true; color = 0xFF047857.toInt() }
        val labelPaint = Paint().apply { textSize = 8f; color = 0xFF64748B.toInt() }
        val valuePaint = Paint().apply { textSize = 8.5f; color = 0xFF0F172A.toInt() }
        val valueBoldPaint = Paint().apply { textSize = 8.5f; isFakeBoldText = true; color = 0xFF0F172A.toInt() }
        val tableHeadPaint = Paint().apply { textSize = 8f; isFakeBoldText = true; color = 0xFF334155.toInt() }
        val tableBodyPaint = Paint().apply { textSize = 8.5f; color = 0xFF0F172A.toInt() }
        val tableSubPaint = Paint().apply { textSize = 7f; color = 0xFF64748B.toInt() }
        val linePaint = Paint().apply { color = 0xFFCBD5E1.toInt(); strokeWidth = 0.8f }
        val darkLinePaint = Paint().apply { color = 0xFF1E293B.toInt(); strokeWidth = 1.5f }
        val bgPaint = Paint().apply { color = 0xFFF8FAFC.toInt() }
        val boxBorderPaint = Paint().apply { color = 0xFFCBD5E1.toInt(); style = Paint.Style.STROKE; strokeWidth = 0.8f }

        var y = MARGIN + 12f

        // 1. Clinic Letterhead Header
        val clinic = invoice.clinic
        val effectiveClinicName = clinic?.name?.ifBlank { null } ?: clinicName.ifBlank { "MedRay Healthcare Clinic" }
        canvas.drawText(effectiveClinicName, MARGIN, y, clinicTitlePaint)
        y += 13f

        val addressText = clinic?.address?.ifBlank { null }
        if (addressText != null) {
            canvas.drawText(addressText, MARGIN, y, clinicMetaPaint)
            y += 11f
        }

        val contactLine = listOfNotNull(
            clinic?.phone?.ifBlank { null }?.let { "Tel: $it" },
            clinic?.upiId?.ifBlank { null }?.let { "UPI: $it" }
        ).joinToString("   ·   ")
        if (contactLine.isNotBlank()) {
            canvas.drawText(contactLine, MARGIN, y, clinicMetaPaint)
            y += 11f
        }

        // Header right-side tax box
        val gstNumber = clinic?.gstNumber?.trim()
        val gstText = if (!gstNumber.isNullOrBlank()) "GSTIN: $gstNumber" else null
        val panNumber = clinic?.panNumber?.trim()?.ifBlank { null }
            ?: if (!gstNumber.isNullOrBlank() && gstNumber.length >= 12) gstNumber.substring(2, 12) else null
        val panText = if (!panNumber.isNullOrBlank()) "PAN: $panNumber" else null
        val stateText = "State: 07-Delhi"
        var rightY = MARGIN + 10f
        if (gstText != null) {
            canvas.drawText(gstText, rightX - taxBadgePaint.measureText(gstText), rightY, taxBadgePaint)
            rightY += 11f
        }
        if (panText != null) {
            canvas.drawText(panText, rightX - taxBadgePaint.measureText(panText), rightY, taxBadgePaint)
            rightY += 11f
        }
        canvas.drawText(stateText, rightX - taxBadgePaint.measureText(stateText), rightY, taxBadgePaint)
        rightY += 11f

        y = maxOf(y + 4f, rightY + 4f)
        canvas.drawLine(MARGIN, y, rightX, y, darkLinePaint)
        y += 14f

        // 2. Document Title & Legal Classification
        canvas.drawText("TAX INVOICE / BILL OF SUPPLY", MARGIN, y, docTitlePaint)
        val statusText = invoice.status.name.replace("_", " ")
        canvas.drawText(statusText, rightX - statusPaint.measureText(statusText), y, statusPaint)
        y += 14f

        // 3. Patient & Encounter Box (2-Column Rounded Box)
        val boxTop = y
        val boxHeight = 54f
        canvas.drawRect(MARGIN, boxTop, rightX, boxTop + boxHeight, bgPaint)
        canvas.drawRect(MARGIN, boxTop, rightX, boxTop + boxHeight, boxBorderPaint)
        val midX = MARGIN + (rightX - MARGIN) / 2f
        canvas.drawLine(midX, boxTop, midX, boxTop + boxHeight, boxBorderPaint)

        // Left Column: Patient
        val patient = invoice.patient
        var py = boxTop + 13f
        canvas.drawText("Patient:", MARGIN + 8f, py, labelPaint)
        canvas.drawText(patient?.fullName ?: "OPD Patient", MARGIN + 52f, py, valueBoldPaint)
        py += 13f
        canvas.drawText("UHID:", MARGIN + 8f, py, labelPaint)
        canvas.drawText(patient?.uhid ?: "—", MARGIN + 52f, py, valuePaint)
        py += 13f
        canvas.drawText("Age / Sex:", MARGIN + 8f, py, labelPaint)
        val ageSex = listOfNotNull(
            patient?.age?.let { "${it}Y" },
            patient?.gender?.lowercase()?.replaceFirstChar { it.titlecase(Locale.ROOT) }
        ).joinToString(" / ").ifBlank { "—" }
        canvas.drawText(ageSex, MARGIN + 52f, py, valuePaint)
        py += 13f
        canvas.drawText("Contact:", MARGIN + 8f, py, labelPaint)
        canvas.drawText(patient?.phone ?: "—", MARGIN + 52f, py, valuePaint)

        // Right Column: Invoice
        var iy = boxTop + 13f
        canvas.drawText("Invoice No:", midX + 8f, iy, labelPaint)
        canvas.drawText("INV-${invoice.invoiceNumber}", midX + 64f, iy, valueBoldPaint)
        iy += 13f
        canvas.drawText("Date & Time:", midX + 8f, iy, labelPaint)
        val dtText = if (!invoice.createdAt.isNullOrBlank()) formatIsoDateTimeLocal(invoice.createdAt) else "—"
        canvas.drawText(dtText, midX + 64f, iy, valuePaint)
        iy += 13f
        canvas.drawText("Encounter:", midX + 8f, iy, labelPaint)
        canvas.drawText("General OPD Consultation", midX + 64f, iy, valuePaint)
        iy += 13f
        canvas.drawText("Dept / Place:", midX + 8f, iy, labelPaint)
        canvas.drawText("Outpatient Department", midX + 64f, iy, valuePaint)

        y = boxTop + boxHeight + 14f

        // 4. Itemized Table
        val colNumX = MARGIN + 6f
        val colDescX = MARGIN + 28f
        val colSacX = rightX - 220f
        val colQtyX = rightX - 150f
        val colGstX = rightX - 100f
        val colAmtX = rightX - 8f

        // Table Header
        canvas.drawRect(MARGIN, y - 2f, rightX, y + 14f, bgPaint)
        canvas.drawLine(MARGIN, y - 2f, rightX, y - 2f, linePaint)
        canvas.drawText("#", colNumX, y + 9f, tableHeadPaint)
        canvas.drawText("Service / Particulars", colDescX, y + 9f, tableHeadPaint)
        canvas.drawText("SAC Code", colSacX, y + 9f, tableHeadPaint)
        canvas.drawText("Qty", colQtyX, y + 9f, tableHeadPaint)
        canvas.drawText("GST Rate", colGstX, y + 9f, tableHeadPaint)
        canvas.drawText("Amount", colAmtX - tableHeadPaint.measureText("Amount"), y + 9f, tableHeadPaint)
        y += 15f
        canvas.drawLine(MARGIN, y, rightX, y, linePaint)
        y += 12f

        invoice.lineItems.forEachIndexed { idx, item ->
            val sacCode = if (item.description.contains("Consult", ignoreCase = true)) "999312" else "999311"
            canvas.drawText("${idx + 1}", colNumX, y, tableSubPaint)
            canvas.drawText(item.description, colDescX, y, tableBodyPaint)
            canvas.drawText(sacCode, colSacX, y, tableSubPaint)
            canvas.drawText("1", colQtyX, y, tableSubPaint)
            canvas.drawText("Exempt (0%)", colGstX, y, tableSubPaint)
            val amountText = "Rs. ${String.format(Locale.ROOT, "%.2f", item.amount)}"
            canvas.drawText(amountText, colAmtX - tableBodyPaint.measureText(amountText), y, tableBodyPaint)
            y += 16f
        }

        if (invoice.discountAmount > 0) {
            canvas.drawText("•", colNumX, y, tableSubPaint)
            canvas.drawText("Special Concession / Discount", colDescX, y, tableBodyPaint)
            canvas.drawText("—", colSacX, y, tableSubPaint)
            canvas.drawText("—", colQtyX, y, tableSubPaint)
            canvas.drawText("—", colGstX, y, tableSubPaint)
            val discText = "-Rs. ${String.format(Locale.ROOT, "%.2f", invoice.discountAmount)}"
            canvas.drawText(discText, colAmtX - tableBodyPaint.measureText(discText), y, tableBodyPaint)
            y += 16f
        }

        canvas.drawLine(MARGIN, y, rightX, y, linePaint)
        y += 12f

        // 5. Amount in Words Box & Totals Box
        val wordsBoxWidth = (rightX - MARGIN) * 0.58f
        val totalsBoxLeft = rightX - 190f

        // Amount in Words
        val wordsRect = RectF(MARGIN, y, MARGIN + wordsBoxWidth, y + 36f)
        canvas.drawRoundRect(wordsRect, 4f, 4f, bgPaint)
        canvas.drawRoundRect(wordsRect, 4f, 4f, boxBorderPaint)
        canvas.drawText("Amount in Words:", MARGIN + 8f, y + 14f, labelPaint)
        val inWords = amountInWords(invoice.total)
        canvas.drawText(inWords, MARGIN + 8f, y + 27f, valueBoldPaint)

        // Totals Rows
        var ty = y + 10f
        fun drawTotalLine(title: String, amount: Double, isBold: Boolean = false, isNet: Boolean = false) {
            val paint = if (isBold) valueBoldPaint else labelPaint
            val valPaint = if (isBold) valueBoldPaint else valuePaint
            canvas.drawText(title, totalsBoxLeft, ty, paint)
            val amtStr = "Rs. ${String.format(Locale.ROOT, "%.2f", amount)}"
            canvas.drawText(amtStr, rightX - valPaint.measureText(amtStr), ty, valPaint)
            ty += if (isNet) 14f else 11f
        }

        drawTotalLine("Subtotal", invoice.subtotal)
        if (invoice.discountAmount > 0) drawTotalLine("Discount", -invoice.discountAmount)
        drawTotalLine("GST (CGST 0% + SGST 0%)", 0.0)
        drawTotalLine("Net Payable", invoice.total, isBold = true, isNet = true)
        drawTotalLine("Total Paid", invoice.netPaid)
        drawTotalLine("Balance Due", invoice.balanceDue, isBold = true)

        y = maxOf(y + 42f, ty + 6f)

        // 6. Payments History (if present)
        if (invoice.payments.isNotEmpty()) {
            canvas.drawLine(MARGIN, y, rightX, y, linePaint)
            y += 11f
            canvas.drawText("Settlement Details", MARGIN, y, tableHeadPaint)
            y += 10f
            invoice.payments.forEach { p ->
                val line = "${formatIsoDateTimeLocal(p.recordedAt)}    Mode: ${p.method.name}    ${p.note?.let { "($it)" } ?: ""}"
                canvas.drawText(line, MARGIN + 6f, y, tableSubPaint)
                val pText = "Rs. ${String.format(Locale.ROOT, "%.2f", p.amount)}"
                canvas.drawText(pText, rightX - tableBodyPaint.measureText(pText), y, tableBodyPaint)
                y += 11f
            }
            y += 4f
        }

        // 7. Sign-off & Footer
        val footerY = PAGE_HEIGHT - MARGIN - 10f
        canvas.drawText("Issued electronically", MARGIN, footerY - 18f, docSubPaint)
        canvas.drawText("This is a computer-generated document and does not require an ink signature.", MARGIN, footerY - 9f, docSubPaint)
        canvas.drawText("Print Timestamp: ${formatIsoDateTimeLocal(java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT).format(java.util.Date()))}", MARGIN, footerY, docSubPaint)

        val sigWidth = 140f
        canvas.drawLine(rightX - sigWidth, footerY - 14f, rightX, footerY - 14f, linePaint)
        val sigLabel = "Authorized Signatory / Cashier"
        canvas.drawText(sigLabel, rightX - valueBoldPaint.measureText(sigLabel), footerY - 4f, valueBoldPaint)
        val forClinic = "For $effectiveClinicName"
        canvas.drawText(forClinic, rightX - docSubPaint.measureText(forClinic), footerY + 5f, docSubPaint)

        document.finishPage(page)
        return document
    }
}
