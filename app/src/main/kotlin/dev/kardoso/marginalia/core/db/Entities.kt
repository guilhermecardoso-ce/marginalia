package dev.kardoso.marginalia.core.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "books", indices = [Index(value = ["uri"], unique = true)])
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** URI do SAF com permissão persistente. */
    val uri: String,
    val title: String,
    val author: String?,
    /** "EPUB" ou "PDF". */
    val format: String,
    @ColumnInfo(name = "media_type") val mediaType: String,
    @ColumnInfo(name = "cover_path") val coverPath: String?,
    /** Última posição de leitura: `Locator` do Readium serializado em JSON. */
    val locator: String?,
    /** Progresso total de 0 a 1. */
    val progression: Double,
    @ColumnInfo(name = "added_at") val addedAt: Long,
    @ColumnInfo(name = "last_opened_at") val lastOpenedAt: Long?,
)

@Entity(
    tableName = "highlights",
    foreignKeys = [
        ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["book_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("book_id")],
)
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "book_id") val bookId: Long,
    val locator: String,
    val text: String,
    val chapter: String?,
    val position: Int?,
    val progression: Double,
    val color: String,
    val note: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["book_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("book_id")],
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "book_id") val bookId: Long,
    val locator: String,
    val chapter: String?,
    val position: Int?,
    val progression: Double,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

data class BookAnnotationCount(
    val bookId: Long,
    val title: String,
    val author: String?,
    @ColumnInfo(name = "cover_path") val coverPath: String?,
    val highlights: Int,
    val notes: Int,
)
