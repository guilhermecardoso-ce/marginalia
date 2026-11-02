package dev.kardoso.marginalia.reader

import android.net.Uri
import dev.kardoso.marginalia.core.prefs.PreferencesRepository
import dev.kardoso.marginalia.core.prefs.ReadingPreferences
import dev.kardoso.marginalia.core.readium.Readium
import dev.kardoso.marginalia.core.readium.locatorFromJson
import dev.kardoso.marginalia.library.Book
import dev.kardoso.marginalia.library.BookFormat
import dev.kardoso.marginalia.library.BookRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.readium.adapter.pdfium.navigator.PdfiumEngineProvider
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.toAbsoluteUrl

/** Uma publicação aberta, pronta para o [ReaderActivity]. */
class ReaderSession(
    val book: Book,
    val publication: Publication,
    /** Última posição conhecida; usada para recriar o navegador. */
    var lastLocator: Locator?,
    var preferences: ReadingPreferences,
) {
    val format: BookFormat get() = book.format

    val epubFactory: EpubNavigatorFactory by lazy { EpubNavigatorFactory(publication) }

    val pdfEngine = PdfiumEngineProvider()

    val pdfFactory by lazy { PdfNavigatorFactory(publication, pdfEngine) }
}

enum class OpenError { NotFound, FileMissing, CannotOpen }

/**
 * Abre e guarda as publicações em leitura. Chame [open] antes de iniciar o [ReaderActivity].
 */
@Singleton
class ReaderRepository @Inject constructor(
    private val readium: Readium,
    private val books: BookRepository,
    private val preferences: PreferencesRepository,
) {
    private val sessions = mutableMapOf<Long, ReaderSession>()
    private val mutex = Mutex()

    operator fun get(bookId: Long): ReaderSession? = sessions[bookId]

    /**
     * @param locatorJson posição para abrir no lugar da última lida (ex.: vinda de uma anotação).
     */
    suspend fun open(bookId: Long, locatorJson: String? = null): Result<ReaderSession> = mutex.withLock {
        val book = books.get(bookId) ?: return Result.failure(OpenException(OpenError.NotFound))
        val prefs = preferences.reading.first()
        val target = locatorFromJson(locatorJson) ?: locatorFromJson(book.locatorJson)

        sessions[bookId]?.let { session ->
            session.lastLocator = target ?: session.lastLocator
            session.preferences = prefs
            return Result.success(session)
        }

        val url = Uri.parse(book.uri).toAbsoluteUrl()
            ?: return Result.failure(OpenException(OpenError.FileMissing))
        val asset = readium.assetRetriever.retrieve(url)
            .getOrElse { return Result.failure(OpenException(OpenError.FileMissing)) }
        val publication = readium.publicationOpener.open(asset, allowUserInteraction = false)
            .getOrElse {
                asset.close()
                return Result.failure(OpenException(OpenError.CannotOpen))
            }

        val session = ReaderSession(book, publication, target, prefs)
        sessions[bookId] = session
        books.markOpened(bookId)
        Result.success(session)
    }

    fun close(bookId: Long) {
        sessions.remove(bookId)?.publication?.close()
    }
}

class OpenException(val error: OpenError) : Exception(error.name)
