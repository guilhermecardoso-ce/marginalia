package dev.kardoso.marginalia.core.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BookEntity::class, HighlightEntity::class, BookmarkEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class MarginaliaDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun annotationDao(): AnnotationDao
}
