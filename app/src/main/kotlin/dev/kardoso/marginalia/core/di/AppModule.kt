package dev.kardoso.marginalia.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.kardoso.marginalia.core.Clock
import dev.kardoso.marginalia.core.db.AnnotationDao
import dev.kardoso.marginalia.core.db.BookDao
import dev.kardoso.marginalia.core.db.MarginaliaDatabase
import javax.inject.Singleton

private val Context.readingDataStore: DataStore<Preferences> by preferencesDataStore(name = "reading")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): MarginaliaDatabase =
        Room.databaseBuilder(context, MarginaliaDatabase::class.java, "marginalia.db").build()

    @Provides
    fun bookDao(db: MarginaliaDatabase): BookDao = db.bookDao()

    @Provides
    fun annotationDao(db: MarginaliaDatabase): AnnotationDao = db.annotationDao()

    @Provides
    @Singleton
    fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.readingDataStore

    @Provides
    fun clock(): Clock = Clock.System
}
