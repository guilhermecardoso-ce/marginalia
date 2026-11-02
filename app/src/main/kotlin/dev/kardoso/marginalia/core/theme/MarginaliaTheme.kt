package dev.kardoso.marginalia.core.theme

import android.provider.Settings
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

val LocalPalette = staticCompositionLocalOf { ReaderTheme.Default.palette }

/** Duração das transições (150–250 ms), zerada quando o sistema remove animações. */
val LocalMotionMillis = staticCompositionLocalOf { 200 }

val MarginaliaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

/**
 * Todo o app usa as cores do tema de leitura ativo: barras, menus e diálogos inclusive.
 */
@Composable
fun MarginaliaTheme(theme: ReaderTheme, content: @Composable () -> Unit) {
    val palette = theme.palette
    val context = LocalContext.current
    val motion = remember(context) {
        val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        if (scale == 0f) 0 else 200
    }
    CompositionLocalProvider(LocalPalette provides palette, LocalMotionMillis provides motion) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(),
            typography = MarginaliaTypography,
            shapes = MarginaliaShapes,
            content = content,
        )
    }
}

private fun ThemePalette.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = brand,
        onPrimary = onBrand,
        primaryContainer = selected,
        onPrimaryContainer = text,
        secondary = text,
        onSecondary = background,
        secondaryContainer = selected,
        onSecondaryContainer = text,
        tertiary = link,
        onTertiary = background,
        background = background,
        onBackground = text,
        surface = background,
        onSurface = text,
        surfaceVariant = selected,
        onSurfaceVariant = secondary,
        surfaceTint = background,
        surfaceBright = background,
        surfaceDim = selected,
        surfaceContainerLowest = background,
        surfaceContainerLow = background,
        surfaceContainer = background,
        surfaceContainerHigh = background,
        surfaceContainerHighest = selected,
        inverseSurface = text,
        inverseOnSurface = background,
        outline = lineStrong,
        outlineVariant = line,
        scrim = text,
    )
}
