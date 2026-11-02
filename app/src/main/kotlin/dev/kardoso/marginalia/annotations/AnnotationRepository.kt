package dev.kardoso.marginalia.annotations

import dev.kardoso.marginalia.core.Clock
import dev.kardoso.marginalia.core.db.AnnotationDao
import dev.kardoso.marginalia.core.db.BookAnnotationCount
import dev.kardoso.marginalia.core.db.BookmarkEntity
import dev.kardoso.marginalia.core.db.HighlightEntity
import dev.kardoso.marginalia.core.theme.HighlightColor
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Highlight(
    val id: Long,
    val bookId: Long,
    val locatorJson: String,
    val text: String,
    val chapter: String?,
    val position: Int?,
    val progression: Double,
    val color: HighlightColor,
    val note: String?,
    val createdAt: Long,
) {
    val hasNote: Boolean get() = !note.isNullOrBlank()
}

data class Bookmark(
    val id: Long,
    val bookId: Long,
    val locatorJson: String,
    val chapter: String?,
    val position: Int?,
    val progression: Double,
    val createdAt: Long,
)

/** Um ponto do livro: de onde nasce um destaque ou um marcador. */
data class BookPoint(
    val locatorJson: String,
    val chapter: String?,
    val position: Int?,
    val progression: Double,
)

@Singleton
class AnnotationRepository @Inject constructor(
    private val dao: AnnotationDao,
    private val clock: Clock,
) {
    fun highlights(bookId: Long): Flow<List<Highlight>> =
        dao.observeHighlights(bookId).map { list -> list.map { it.toHighlight() } }

    suspend fun highlightsOnce(bookId: Long): List<Highlight> = dao.highlights(bookId).map { it.toHighlight() }

    suspend fun highlight(id: Long): Highlight? = dao.highlight(id)?.toHighlight()

    fun annotatedBooks(): Flow<List<BookAnnotationCount>> = dao.observeAnnotationCounts()

    suspend fun addHighlight(
        bookId: Long,
        point: BookPoint,
        text: String,
        color: HighlightColor,
        note: String? = null,
    ): Long = dao.insertHighlight(
        HighlightEntity(
            bookId = bookId,
            locator = point.locatorJson,
            text = text.trim(),
            chapter = point.chapter,
            position = point.position,
            progression = point.progression,
            color = color.name,
            note = note.normalizedNote(),
            createdAt = clock.now(),
        ),
    )

    suspend fun setNote(id: Long, note: String?) = dao.updateNote(id, note.normalizedNote())

    suspend fun setColor(id: Long, color: HighlightColor) = dao.updateColor(id, color.name)

    suspend fun deleteHighlight(id: Long) = dao.deleteHighlight(id)

    fun bookmarks(bookId: Long): Flow<List<Bookmark>> =
        dao.observeBookmarks(bookId).map { list -> list.map { it.toBookmark() } }

    suspend fun deleteBookmark(id: Long) = dao.deleteBookmark(id)

    /**
     * Marca ou desmarca a página atual. Devolve `true` se a página ficou marcada.
     */
    suspend fun toggleBookmark(bookId: Long, point: BookPoint, existing: List<Bookmark>): Boolean {
        val match = findBookmarkAt(point, existing)
        if (match != null) {
            dao.deleteBookmark(match.id)
            return false
        }
        dao.insertBookmark(
            BookmarkEntity(
                bookId = bookId,
                locator = point.locatorJson,
                chapter = point.chapter,
                position = point.position,
                progression = point.progression,
                createdAt = clock.now(),
            ),
        )
        return true
    }

    companion object {
        private const val SAME_PAGE_TOLERANCE = 0.0005

        fun findBookmarkAt(point: BookPoint, bookmarks: List<Bookmark>): Bookmark? =
            bookmarks.firstOrNull { bookmark ->
                if (point.position != null && bookmark.position != null) {
                    bookmark.position == point.position
                } else {
                    abs(bookmark.progression - point.progression) < SAME_PAGE_TOLERANCE
                }
            }
    }
}

private fun String?.normalizedNote(): String? = this?.trim()?.ifEmpty { null }

private fun HighlightEntity.toHighlight() = Highlight(
    id = id,
    bookId = bookId,
    locatorJson = locator,
    text = text,
    chapter = chapter,
    position = position,
    progression = progression,
    color = HighlightColor.fromName(color),
    note = note,
    createdAt = createdAt,
)

private fun BookmarkEntity.toBookmark() = Bookmark(
    id = id,
    bookId = bookId,
    locatorJson = locator,
    chapter = chapter,
    position = position,
    progression = progression,
    createdAt = createdAt,
)
