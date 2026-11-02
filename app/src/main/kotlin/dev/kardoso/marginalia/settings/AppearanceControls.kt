package dev.kardoso.marginalia.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kardoso.marginalia.R
import dev.kardoso.marginalia.core.prefs.LineSpacing
import dev.kardoso.marginalia.core.prefs.ReaderFont
import dev.kardoso.marginalia.core.prefs.ReadingPreferences
import dev.kardoso.marginalia.core.theme.Inter
import dev.kardoso.marginalia.core.theme.Literata
import dev.kardoso.marginalia.core.theme.LocalPalette
import dev.kardoso.marginalia.core.theme.ReaderTheme
import dev.kardoso.marginalia.core.ui.SectionLabel
import dev.kardoso.marginalia.core.ui.Segmented

/**
 * Tema, tamanho, fonte e entrelinha. Usado na folha "Aparência" do leitor e em Ajustes;
 * cada mudança é aplicada na hora.
 */
@Composable
fun AppearanceControls(
    prefs: ReadingPreferences,
    onChange: (ReadingPreferences) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    /** PDF não reflui: só o tema se aplica. */
    showTextOptions: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp), modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(stringResource(R.string.appearance_theme))
            ThemeSwatches(selected = prefs.theme, onSelect = { onChange(prefs.copy(theme = it)) })
        }

        if (showTextOptions) TextOptions(prefs, onChange)

        TextButton(onClick = onReset, contentPadding = PaddingValues(0.dp), modifier = Modifier.heightIn(min = 44.dp)) {
            Text(
                stringResource(R.string.appearance_reset),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline,
                ),
                color = LocalPalette.current.secondary,
            )
        }
    }
}

@Composable
private fun TextOptions(prefs: ReadingPreferences, onChange: (ReadingPreferences) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            SectionLabel(stringResource(R.string.appearance_size), modifier = Modifier.weight(1f))
            FontSizeStepper(
                size = prefs.fontSizeSp,
                onSmaller = { onChange(prefs.withFontSize(prefs.fontSizeSp - 1)) },
                onBigger = { onChange(prefs.withFontSize(prefs.fontSizeSp + 1)) },
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(stringResource(R.string.appearance_font))
            Segmented(
                options = ReaderFont.entries,
                selected = prefs.font,
                onSelect = { onChange(prefs.copy(font = it)) },
            ) { font, isSelected ->
                Text(
                    text = font.readiumName,
                    style = TextStyle(
                        fontFamily = if (font == ReaderFont.LITERATA) Literata else Inter,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    ),
                    color = LocalPalette.current.text,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(stringResource(R.string.appearance_line_spacing))
            Segmented(
                options = LineSpacing.entries,
                selected = prefs.lineSpacing,
                onSelect = { onChange(prefs.copy(lineSpacing = it)) },
            ) { spacing, isSelected ->
                Text(
                    text = stringResource(spacing.label()),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = LocalPalette.current.text,
                )
            }
        }
    }
}

@Composable
fun ThemeSwatches(selected: ReaderTheme, onSelect: (ReaderTheme) -> Unit) {
    val current = LocalPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        ReaderTheme.entries.forEach { theme ->
            val palette = theme.palette
            val isSelected = theme == selected
            val name = stringResource(theme.label())
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(theme) }
                    .semantics { contentDescription = name },
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(palette.background)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) current.brand else current.line,
                            shape = MaterialTheme.shapes.medium,
                        ),
                ) {
                    Text("Aa", style = TextStyle(fontFamily = Literata, fontSize = 20.sp, color = palette.text))
                }
                Text(name, style = MaterialTheme.typography.labelMedium, color = current.text)
            }
        }
    }
}

@Composable
private fun FontSizeStepper(size: Int, onSmaller: () -> Unit, onBigger: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.border(1.dp, palette.line, MaterialTheme.shapes.medium),
    ) {
        TextButton(
            onClick = onSmaller,
            enabled = size > ReadingPreferences.MIN_FONT_SIZE,
            modifier = Modifier.size(width = 56.dp, height = 44.dp),
        ) {
            val description = stringResource(R.string.appearance_smaller)
            Text(
                "A−",
                style = TextStyle(fontFamily = Literata, fontSize = 15.sp),
                color = palette.text,
                modifier = Modifier.semantics { contentDescription = description },
            )
        }
        Text(
            text = size.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = palette.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 48.dp),
        )
        TextButton(
            onClick = onBigger,
            enabled = size < ReadingPreferences.MAX_FONT_SIZE,
            modifier = Modifier.size(width = 56.dp, height = 44.dp),
        ) {
            val description = stringResource(R.string.appearance_bigger)
            Text(
                "A+",
                style = TextStyle(fontFamily = Literata, fontSize = 20.sp),
                color = palette.text,
                modifier = Modifier.semantics { contentDescription = description },
            )
        }
    }
}

fun ReaderTheme.label(): Int = when (this) {
    ReaderTheme.CLARO -> R.string.theme_light
    ReaderTheme.CLASSICO -> R.string.theme_classic
    ReaderTheme.SEPIA -> R.string.theme_sepia
    ReaderTheme.ESCURO -> R.string.theme_dark
}

fun LineSpacing.label(): Int = when (this) {
    LineSpacing.COMPACTA -> R.string.spacing_compact
    LineSpacing.PADRAO -> R.string.spacing_default
    LineSpacing.AMPLA -> R.string.spacing_wide
}
