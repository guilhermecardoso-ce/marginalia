package dev.kardoso.marginalia.core.readium

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.kardoso.marginalia.core.prefs.ReadingPreferences
import dev.kardoso.marginalia.core.theme.ReaderTheme
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject
import org.readium.adapter.pdfium.document.PdfiumDocumentFactory
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.Color
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.ImageFilter
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

/** Objetos compartilhados do Readium. Só arquivos locais: nada é baixado da rede. */
@Singleton
class Readium @Inject constructor(@ApplicationContext context: Context) {

    private val httpClient = DefaultHttpClient()

    val assetRetriever = AssetRetriever(context.contentResolver, httpClient)

    val publicationOpener = PublicationOpener(
        publicationParser = DefaultPublicationParser(
            context,
            httpClient = httpClient,
            assetRetriever = assetRetriever,
            pdfFactory = PdfiumDocumentFactory(context),
        ),
    )
}

fun Locator.toJsonString(): String = toJSON().toString()

fun locatorFromJson(json: String?): Locator? =
    json?.let { runCatching { Locator.fromJSON(JSONObject(it)) }.getOrNull() }

/**
 * Converte as preferências do app nas preferências do navegador EPUB. As cores do tema vão por
 * `backgroundColor`/`textColor`, nunca por CSS injetado.
 */
fun ReadingPreferences.toEpubPreferences(): EpubPreferences {
    val palette = theme.palette
    return EpubPreferences(
        backgroundColor = Color(palette.background.toArgb()),
        textColor = Color(palette.text.toArgb()),
        fontFamily = FontFamily(font.readiumName),
        // A fonte base do ReadiumCSS é 16 px: 18 sp corresponde a 1,125.
        fontSize = fontSizeSp / BASE_FONT_SIZE,
        lineHeight = lineSpacing.value,
        hyphens = true,
        textAlign = TextAlign.START,
        publisherStyles = false,
        // Escuro: imagens escurecidas, nunca invertidas.
        imageFilter = if (theme == ReaderTheme.ESCURO) ImageFilter.DARKEN else null,
    )
}

private const val BASE_FONT_SIZE = 16.0
