package dev.kardoso.marginalia

import dev.kardoso.marginalia.core.db.AnnotationDao
import dev.kardoso.marginalia.core.db.BookAnnotationCount
import dev.kardoso.marginalia.core.db.BookDao
import dev.kardoso.marginalia.core.db.BookEntity
import dev.kardoso.marginalia.core.db.BookmarkEntity
import dev.kardoso.marginalia.core.db.HighlightEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeBookDao : BookDao {
    val books = MutableStateFlow<List<BookEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<BookEntity>> =
        books.map { list -> list.sortedByDescending { it.lastOpenedAt ?: it.addedAt } }

    override fun observe(id: Long): Flow<BookEntity?> = books.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun get(id: Long): BookEntity? = books.value.firstOrNull { it.id == id }

    override suspend fun findByUri(uri: String): BookEntity? = books.value.firstOrNull { it.uri == uri }

    override suspend fun insert(book: BookEntity): Long {
        val id = nextId++
        books.value += book.copy(id = id)
        return id
    }

    override suspend fun updateProgress(id: Long, locator: String, progression: Double, openedAt: Long) =
        update(id) { it.copy(locator = locator, progression = progression, lastOpenedAt = openedAt) }

    override suspend fun markOpened(id: Long, openedAt: Long) = update(id) { it.copy(lastOpenedAt = openedAt) }

    override suspend fun delete(id: Long) {
        books.value = books.value.filterNot { it.id == id }
    }

    private fun update(id: Long, transform: (BookEntity) -> BookEntity) {
        books.value = books.value.map { if (it.id == id) transform(it) else it }
    }
}

class FakeAnnotationDao : AnnotationDao {
    val highlights = MutableStateFlow<List<HighlightEntity>>(emptyList())
    val bookmarks = MutableStateFlow<List<BookmarkEntity>>(emptyList())
    private var nextId = 1L

    override fun observeHighlights(bookId: Long): Flow<List<HighlightEntity>> =
        highlights.map { list -> list.filter { it.bookId == bookId }.sortedWith(compareBy({ it.progression }, { it.id })) }

    override suspend fun highlights(bookId: Long): List<HighlightEntity> =
        highlights.value.filter { it.bookId == bookId }.sortedWith(compareBy({ it.progression }, { it.id }))

    override suspend fun highlight(id: Long): HighlightEntity? = highlights.value.firstOrNull { it.id == id }

    override suspend fun insertHighlight(highlight: HighlightEntity): Long {
        val id = nextId++
        highlights.value += highlight.copy(id = id)
        return id
    }

    override suspend fun updateNote(id: Long, note: String?) {
        highlights.value = highlights.value.map { if (it.id == id) it.copy(note = note) else it }
    }

    override suspend fun updateColor(id: Long, color: String) {
        highlights.value = highlights.value.map { if (it.id == id) it.copy(color = color) else it }
    }

    override suspend fun deleteHighlight(id: Long) {
        highlights.value = highlights.value.filterNot { it.id == id }
    }

    override fun observeBookmarks(bookId: Long): Flow<List<BookmarkEntity>> =
        bookmarks.map { list -> list.filter { it.bookId == bookId } }

    override suspend fun insertBookmark(bookmark: BookmarkEntity): Long {
        val id = nextId++
        bookmarks.value += bookmark.copy(id = id)
        return id
    }

    override suspend fun deleteBookmark(id: Long) {
        bookmarks.value = bookmarks.value.filterNot { it.id == id }
    }

    override fun observeAnnotationCounts(): Flow<List<BookAnnotationCount>> = MutableStateFlow(emptyList())
}
