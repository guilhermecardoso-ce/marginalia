package dev.kardoso.marginalia.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY COALESCE(last_opened_at, added_at) DESC")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun observe(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun get(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE uri = :uri")
    suspend fun findByUri(uri: String): BookEntity?

    @Insert
    suspend fun insert(book: BookEntity): Long

    @Query("UPDATE books SET locator = :locator, progression = :progression, last_opened_at = :openedAt WHERE id = :id")
    suspend fun updateProgress(id: Long, locator: String, progression: Double, openedAt: Long)

    @Query("UPDATE books SET last_opened_at = :openedAt WHERE id = :id")
    suspend fun markOpened(id: Long, openedAt: Long)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface AnnotationDao {
    @Query("SELECT * FROM highlights WHERE book_id = :bookId ORDER BY progression, id")
    fun observeHighlights(bookId: Long): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights WHERE book_id = :bookId ORDER BY progression, id")
    suspend fun highlights(bookId: Long): List<HighlightEntity>

    @Query("SELECT * FROM highlights WHERE id = :id")
    suspend fun highlight(id: Long): HighlightEntity?

    @Insert
    suspend fun insertHighlight(highlight: HighlightEntity): Long

    @Query("UPDATE highlights SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String?)

    @Query("UPDATE highlights SET color = :color WHERE id = :id")
    suspend fun updateColor(id: Long, color: String)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteHighlight(id: Long)

    @Query("SELECT * FROM bookmarks WHERE book_id = :bookId ORDER BY progression, id")
    fun observeBookmarks(bookId: Long): Flow<List<BookmarkEntity>>

    @Insert
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Long)

    @Query(
        """
        SELECT b.id AS bookId, b.title AS title, b.author AS author, b.cover_path AS cover_path,
               COUNT(h.id) AS highlights,
               SUM(CASE WHEN h.note IS NOT NULL AND h.note != '' THEN 1 ELSE 0 END) AS notes
        FROM books b JOIN highlights h ON h.book_id = b.id
        GROUP BY b.id
        ORDER BY MAX(h.created_at) DESC
        """,
    )
    fun observeAnnotationCounts(): Flow<List<BookAnnotationCount>>
}
