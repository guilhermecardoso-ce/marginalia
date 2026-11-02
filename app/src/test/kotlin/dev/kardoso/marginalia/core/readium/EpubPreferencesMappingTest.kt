package dev.kardoso.marginalia.core.readium

import androidx.compose.ui.graphics.toArgb
import dev.kardoso.marginalia.core.prefs.LineSpacing
import dev.kardoso.marginalia.core.prefs.ReaderFont
import dev.kardoso.marginalia.core.prefs.ReadingPreferences
import dev.kardoso.marginalia.core.theme.ReaderTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.readium.r2.navigator.preferences.ImageFilter
import org.readium.r2.navigator.preferences.TextAlign

class EpubPreferencesMappingTest {

    @Test
    fun `padrões do produto viram preferências do Readium`() {
        val prefs = ReadingPreferences().toEpubPreferences()

        assertEquals(ReaderTheme.CLASSICO.palette.background.toArgb(), prefs.backgroundColor?.int)
        assertEquals(ReaderTheme.CLASSICO.palette.text.toArgb(), prefs.textColor?.int)
        assertEquals("Literata", prefs.fontFamily?.name)
        assertEquals(1.125, prefs.fontSize)
        assertEquals(1.5, prefs.lineHeight)
        assertEquals(true, prefs.hyphens)
        assertEquals(TextAlign.START, prefs.textAlign)
        assertEquals(false, prefs.publisherStyles)
        assertNull(prefs.imageFilter)
    }

    @Test
    fun `tema escuro escurece imagens sem inverter`() {
        val prefs = ReadingPreferences(theme = ReaderTheme.ESCURO, font = ReaderFont.INTER, lineSpacing = LineSpacing.AMPLA)
            .toEpubPreferences()

        assertEquals(ImageFilter.DARKEN, prefs.imageFilter)
        assertEquals("Inter", prefs.fontFamily?.name)
        assertEquals(1.8, prefs.lineHeight)
    }

    @Test
    fun `tamanho da fonte fica nos limites`() {
        assertEquals(ReadingPreferences.MAX_FONT_SIZE, ReadingPreferences().withFontSize(99).fontSizeSp)
        assertEquals(ReadingPreferences.MIN_FONT_SIZE, ReadingPreferences().withFontSize(1).fontSizeSp)
    }
}
