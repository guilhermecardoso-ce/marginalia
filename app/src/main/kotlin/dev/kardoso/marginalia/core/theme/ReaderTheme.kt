package dev.kardoso.marginalia.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Os quatro temas de leitura. Os valores de fundo, texto, secundário e link são fixos (CLAUDE.md);
 * os demais derivam deles e estão documentados em docs/BRAND.md.
 */
enum class ReaderTheme(val palette: ThemePalette) {
    CLARO(
        ThemePalette(
            background = Color(0xFFFAFAF7),
            text = Color(0xFF1F1F1F),
            secondary = Color(0xFF6B6B6B),
            link = Color(0xFF2F5DA8),
            line = Color(0xFFE3E3DE),
            lineStrong = Color(0xFFD4D4CE),
            selected = Color(0xFFECECE7),
            code = Color(0xFFF0F0EC),
            brand = Color(0xFF7A3B1D),
            onBrand = Color(0xFFFAFAF7),
            isDark = false,
        ),
    ),
    CLASSICO(
        ThemePalette(
            background = Color(0xFFFBF8F1),
            text = Color(0xFF262321),
            secondary = Color(0xFF6F6A62),
            link = Color(0xFF7A3B1D),
            line = Color(0xFFE6DFD0),
            lineStrong = Color(0xFFD6CDBB),
            selected = Color(0xFFEDE6D7),
            code = Color(0xFFF3EEE3),
            brand = Color(0xFF7A3B1D),
            onBrand = Color(0xFFFBF8F1),
            isDark = false,
        ),
    ),
    SEPIA(
        ThemePalette(
            background = Color(0xFFF1E4C9),
            text = Color(0xFF3B2E22),
            secondary = Color(0xFF6E5A44),
            link = Color(0xFF8A4517),
            line = Color(0xFFD9C6A0),
            lineStrong = Color(0xFFC9B48A),
            selected = Color(0xFFE3D3B1),
            code = Color(0xFFE8D8B8),
            brand = Color(0xFF7A3B1D),
            onBrand = Color(0xFFF1E4C9),
            isDark = false,
        ),
    ),
    ESCURO(
        ThemePalette(
            background = Color(0xFF121212),
            text = Color(0xFFCFCAC1),
            secondary = Color(0xFF8F8B84),
            link = Color(0xFF8AB4F8),
            line = Color(0xFF2C2B29),
            lineStrong = Color(0xFF3A3936),
            selected = Color(0xFF2A2927),
            code = Color(0xFF1C1C1B),
            brand = Color(0xFFD08A62),
            onBrand = Color(0xFF121212),
            isDark = true,
        ),
    ),
    ;

    companion object {
        val Default = CLASSICO
    }
}

data class ThemePalette(
    val background: Color,
    val text: Color,
    val secondary: Color,
    val link: Color,
    val line: Color,
    val lineStrong: Color,
    val selected: Color,
    val code: Color,
    val brand: Color,
    val onBrand: Color,
    val isDark: Boolean,
) {
    /** Véu atrás de folhas modais: texto a 24%. */
    val scrim: Color get() = text.copy(alpha = 0.24f)
}

/** Cores de destaque das anotações. No tema escuro, versões de baixa luminância. */
enum class HighlightColor(val light: Color, val dark: Color) {
    AMARELO(Color(0xFFF2DC8C), Color(0xFF5C4D1A)),
    VERDE(Color(0xFFCFE3B4), Color(0xFF33492A)),
    AZUL(Color(0xFFC9DBF2), Color(0xFF283E57)),
    ROSA(Color(0xFFF2CBC4), Color(0xFF57302A)),
    ;

    fun on(palette: ThemePalette): Color = if (palette.isDark) dark else light

    companion object {
        fun fromName(name: String?): HighlightColor = entries.firstOrNull { it.name == name } ?: AMARELO
    }
}
