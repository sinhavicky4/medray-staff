package ai.medray.staff.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/**
 * Flowing wave artwork matching MedRay clinical design system —
 * layered translucent sine ribbons anchored to the bottom of the screen.
 */
@Composable
fun WavyBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        fun wave(baseY: Float, amplitude: Float, color: Color) {
            val path = Path().apply {
                moveTo(0f, baseY)
                cubicTo(w * 0.25f, baseY - amplitude, w * 0.35f, baseY + amplitude, w * 0.55f, baseY)
                cubicTo(w * 0.75f, baseY - amplitude * 1.2f, w * 0.85f, baseY + amplitude, w, baseY - amplitude * 0.4f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(path, color = color)
        }

        wave(h * 0.78f, 26f, Color(0xFF3B82F6).copy(alpha = 0.10f))
        wave(h * 0.85f, 34f, Color(0xFF60A5FA).copy(alpha = 0.14f))
        wave(h * 0.92f, 20f, Color(0xFF93C5FD).copy(alpha = 0.20f))
    }
}
