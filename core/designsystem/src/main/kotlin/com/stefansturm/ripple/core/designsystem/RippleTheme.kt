package com.stefansturm.ripple.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.provider.Settings

object RippleColors {
    val waterDeep = Color(0xFF0B3D4A)
    val waterDeepDark = Color(0xFFB7E0E8)
    val lagoon = Color(0xFF1A7A8C)
    val lagoonDark = Color(0xFF4FB3C6)
    val aqua = Color(0xFF4FB3C6)
    val aquaDark = Color(0xFF6FDBE8)
    val foam = Color(0xFFE8F4F6)
    val foamDark = Color(0xFF152026)
    val surface = Color(0xFFF8FCFD)
    val surfaceDark = Color(0xFF0E171A)
    val elevated = Color(0xFFFFFFFF)
    val elevatedDark = Color(0xFF1C292D)
    val selected = Color(0xFFD5EEF2)
    val selectedDark = Color(0xFF244950)
    val mutedIcon = Color(0xFF5D7378)
    val mutedIconDark = Color(0xFFA1B9BD)
    val onAction = Color.White
    val danger = Color(0xFFBA4D4D)
}

object RippleMetrics {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 40.dp
    val heroMin = 168.dp
    val controlMin = 48.dp
    val icon = 24.dp
    val smallIcon = 20.dp
    val cardRadius = 20.dp
    val controlRadius = 12.dp
    val heroRadius = 28.dp
    val heroMaxWidth = 240.dp
    val heroMaxHeight = 336.dp
    val heroWidthFraction = 0.72f
    val heroRimToBaseRatio = 1.35f
    val heroBottomRadius = 24.dp
    val heroStroke = 1.5.dp
    val heroInnerStroke = 1.dp
    val heroWaterStroke = 2.dp
    val pourStreamAboveGlass = 32.dp
    val chartHeight = 180.dp
}

object RippleMotion {
    const val quickMillis = 280
    const val pourMillis = 520
    const val settleMillis = 900
    const val confirmationMillis = 1300
    const val undoMillis = 450
    const val crossfadeMillis = 200
}

val LocalRippleReducedMotion = staticCompositionLocalOf { false }

private val LightRippleScheme = lightColorScheme(
    primary = RippleColors.lagoon,
    onPrimary = RippleColors.onAction,
    primaryContainer = RippleColors.selected,
    onPrimaryContainer = RippleColors.waterDeep,
    secondary = RippleColors.aqua,
    onSecondary = RippleColors.waterDeep,
    background = RippleColors.foam,
    onBackground = RippleColors.waterDeep,
    surface = RippleColors.surface,
    onSurface = RippleColors.waterDeep,
    surfaceVariant = RippleColors.foam,
    onSurfaceVariant = RippleColors.mutedIcon,
    error = RippleColors.danger
)

private val DarkRippleScheme = darkColorScheme(
    primary = RippleColors.lagoonDark,
    onPrimary = RippleColors.waterDeep,
    primaryContainer = RippleColors.selectedDark,
    onPrimaryContainer = RippleColors.waterDeepDark,
    secondary = RippleColors.aquaDark,
    onSecondary = RippleColors.waterDeep,
    background = RippleColors.foamDark,
    onBackground = RippleColors.waterDeepDark,
    surface = RippleColors.surfaceDark,
    onSurface = RippleColors.waterDeepDark,
    surfaceVariant = RippleColors.elevatedDark,
    onSurfaceVariant = RippleColors.mutedIconDark,
    error = Color(0xFFFFB4AB)
)

private val RippleTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Light, fontSize = 48.sp, lineHeight = 56.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Light, fontSize = 40.sp, lineHeight = 48.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontSize = 12.sp, lineHeight = 16.sp)
)

@Composable
fun RippleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemReducedMotion = remember(context) {
        runCatching {
            val resolver = context.contentResolver
            Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f ||
                Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    CompositionLocalProvider(LocalRippleReducedMotion provides (reducedMotion || systemReducedMotion)) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkRippleScheme else LightRippleScheme,
            typography = RippleTypography,
            shapes = Shapes(
                extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(RippleMetrics.controlRadius),
                small = androidx.compose.foundation.shape.RoundedCornerShape(RippleMetrics.controlRadius),
                medium = androidx.compose.foundation.shape.RoundedCornerShape(RippleMetrics.cardRadius),
                large = androidx.compose.foundation.shape.RoundedCornerShape(RippleMetrics.heroRadius),
                extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(RippleMetrics.heroRadius)
            ),
            content = content
        )
    }
}
