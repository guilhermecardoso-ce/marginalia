package dev.kardoso.marginalia.core.prefs

import dev.kardoso.marginalia.core.theme.ReaderTheme

enum class ReaderFont(val readiumName: String) {
    LITERATA("Literata"),
    INTER("Inter"),
}

enum class LineSpacing(val value: Double) {
    COMPACTA(1.3),
    PADRAO(1.5),
    AMPLA(1.8),
}

/** Preferências globais de leitura. Padrões do CLAUDE.md: Clássico, Literata 18 sp, entrelinha 1,5. */
data class ReadingPreferences(
    val theme: ReaderTheme = ReaderTheme.Default,
    val fontSizeSp: Int = DEFAULT_FONT_SIZE,
    val font: ReaderFont = ReaderFont.LITERATA,
    val lineSpacing: LineSpacing = LineSpacing.PADRAO,
) {
    fun withFontSize(sp: Int) = copy(fontSizeSp = sp.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE))

    companion object {
        const val DEFAULT_FONT_SIZE = 18
        const val MIN_FONT_SIZE = 14
        const val MAX_FONT_SIZE = 28
    }
}
