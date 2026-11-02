package dev.kardoso.marginalia.library

import dev.kardoso.marginalia.core.Clock
import dev.kardoso.marginalia.core.db.BookDao
import dev.kardoso.marginalia.core.db.BookEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class BookFormat { EPUB, PDF }

data class Book(
    val id: Long,
    val uri: String,
    val title: String,
    val author: String?,
    val format: BookFormat,
    val mediaType: String,
    val coverPath: String?,
    val locatorJson: String?,
    val progression: Double,
    val addedAt: Long,
    val lastOpenedAt: Long?,
) {
    val isStarted: Boolean get() = locatorJson != null && progression > 0.0
    val isFinished: Boolean get() = progression >= FINISHED_THRESHOLD

    companion object {
        const val FINISHED_THRESHOLD = 0.995
    }
}

data class NewBook(
    val uri: String,
    val title: String,
    val author: String?,
    val format: BookFormat,
    val mediaType: String,
    val coverPath: String?,
)

sealed interface AddBookResult {
    val id: Long

    data class Added(override val id: Long) : AddBookResult
    data class AlreadyInLibrary(override val id: Long) : AddBookResult
}

@Singleton
class BookRepository @Inject constructor(
    private val dao: BookDao,
    private val clock: Clock,
) {
    val books: Flow<List<Book>> = dao.observeAll().map { list -> list.map { it.toBook() } }

    fun observe(id: Long): Flow<Book?> = dao.observe(id).map { it?.toBook() }

    suspend fun get(id: Long): Book? = dao.get(id)?.toBook()

    suspend fun findByUri(uri: String): Book? = dao.findByUri(uri)?.toBook()

    suspend fun add(book: NewBook): AddBookResult {
        dao.findByUri(book.uri)?.let { return AddBookResult.AlreadyInLibrary(it.id) }
        val title = book.title.trim().ifEmpty { "Sem título" }
        val id = dao.insert(
            BookEntity(
                uri = book.uri,
                title = title,
                author = book.author?.trim()?.ifEmpty { null },
                format = book.format.name,
                mediaType = book.mediaType,
                coverPath = book.coverPath,
                locator = null,
                progression = 0.0,
                addedAt = clock.now(),
                lastOpenedAt = null,
            ),
        )
        return AddBookResult.Added(id)
    }

    /** Salva a posição a cada mudança de página e ao sair do leitor. */
    suspend fun saveProgress(id: Long, locatorJson: String, progression: Double?) {
        val clamped = (progression ?: 0.0).takeIf { it.isFinite() }?.coerceIn(0.0, 1.0) ?: 0.0
        dao.updateProgress(id, locatorJson, clamped, clock.now())
    }

    suspend fun markOpened(id: Long) = dao.markOpened(id, clock.now())

    suspend fun delete(id: Long) = dao.delete(id)
}

private fun BookEntity.toBook() = Book(
    id = id,
    uri = uri,
    title = title,
    author = author,
    format = BookFormat.entries.firstOrNull { it.name == format } ?: BookFormat.EPUB,
    mediaType = mediaType,
    coverPath = coverPath,
    locatorJson = locator,
    progression = progression,
    addedAt = addedAt,
    lastOpenedAt = lastOpenedAt,
)
