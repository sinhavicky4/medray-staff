package ai.medray.staff.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = MedRayBluePrimary,
    onPrimary = PureWhite,
    primaryContainer = MedRayBlueContainer,
    onPrimaryContainer = MedRayBlueDarker,
    secondary = MedRayTealPrimary,
    onSecondary = PureWhite,
    secondaryContainer = MedRayTealContainer,
    onSecondaryContainer = MedRayTealDark,
    tertiary = Slate700,
    onTertiary = PureWhite,
    background = Slate50,
    onBackground = Slate900,
    surface = PureWhite,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    outline = Slate300,
    outlineVariant = Slate200,
    error = StatusErrorText,
    onError = PureWhite,
    errorContainer = StatusErrorBg,
    onErrorContainer = StatusErrorText
)

@Composable
fun MedRayStaffTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
