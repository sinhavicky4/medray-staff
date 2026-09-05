package ai.medray.staff.ui.splash

import ai.medray.staff.R
import ai.medray.staff.core.config.BrandConfig
import ai.medray.staff.ui.common.WavyBackground
import ai.medray.staff.ui.theme.HeadingFontFamily
import ai.medray.staff.ui.theme.InterFontFamily
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val SplashBlue = Color(0xFF2563EB)
private val SplashDark = Color(0xFF0F172A)
private val SplashBgStart = Color(0xFFF8FAFC)
private val SplashBgEnd = Color(0xFFEFF6FF)

/**
 * Animated clinical splash screen with brand lockup and system initialization feedback,
 * perfectly synchronized with the Doctor Tablet experience.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "splashAlpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.85f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "splashScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1800)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SplashBgStart, Color(0xFFEEF2FF), SplashBgEnd))),
        contentAlignment = Alignment.Center
    ) {
        WavyBackground(modifier = Modifier.fillMaxSize())

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .alpha(alphaAnim)
                .scale(scaleAnim)
                .padding(32.dp)
        ) {
            // Animated Logo Container
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(pulseScale)
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .border(2.dp, Color(0xFFDBEAFE), RoundedCornerShape(24.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_medray_logo),
                    contentDescription = "MedRay AI",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(20.dp))

            // Brand Title in Plus Jakarta Sans
            Text(
                BrandConfig.APP_NAME,
                fontFamily = HeadingFontFamily,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = SplashDark
            )

            Spacer(Modifier.height(8.dp))

            // Tagline Pill
            Row(
                modifier = Modifier
                    .background(Color(0xFFEFF6FF), RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(6.dp).background(SplashBlue, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(
                    "NURSES & FRONT DESK MOBILE WORKSPACE",
                    fontFamily = HeadingFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SplashBlue
                )
            }

            Spacer(Modifier.height(36.dp))

            // Loading Spinner
            CircularProgressIndicator(
                color = SplashBlue,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.height(14.dp))

            Text(
                "Initializing Clinical Workspace...",
                fontFamily = InterFontFamily,
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium
            )
        }

        // Bottom Compliance & Security Guarantee
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
                .alpha(alphaAnim),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Security,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "256-bit Encrypted & Secure Clinical Records",
                fontFamily = InterFontFamily,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
