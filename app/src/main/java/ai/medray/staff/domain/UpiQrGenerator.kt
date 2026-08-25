package ai.medray.staff.domain

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.EnumMap

object UpiQrGenerator {

    /**
     * Constructs a standard NPCI compliant UPI Intent URI string:
     * upi://pay?pa={vpa}&pn={payeeName}&am={amount}&tn={note}&tr={txRef}&cu=INR
     */
    fun createUpiUri(
        payeeVpa: String,
        payeeName: String,
        amount: Double,
        invoiceNumber: String,
        note: String = "Medical Consultation"
    ): String {
        val encodedName = URLEncoder.encode(payeeName, StandardCharsets.UTF_8.toString())
        val formattedAmount = String.format("%.2f", amount)
        val encodedNote = URLEncoder.encode("$note #$invoiceNumber", StandardCharsets.UTF_8.toString())
        val txRef = "MR-${invoiceNumber.replace("[^A-Za-z0-9]".toRegex(), "")}"

        return "upi://pay?pa=$payeeVpa&pn=$encodedName&am=$formattedAmount&tn=$encodedNote&tr=$txRef&cu=INR"
    }

    /**
     * Generates a square QR Code Bitmap from the payload string using ZXing.
     */
    fun generateQrBitmap(payload: String, sizePx: Int = 512): Bitmap {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
        hints[EncodeHintType.MARGIN] = 1
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"

        val bitMatrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
