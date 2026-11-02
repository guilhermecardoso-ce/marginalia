package dev.kardoso.marginalia.library

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.kardoso.marginalia.core.readium.Readium
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.toAbsoluteUrl

sealed interface ImportResult {
    data class Added(val bookId: Long, val title: String) : ImportResult
    data class AlreadyInLibrary(val bookId: Long) : ImportResult
    data object Unsupported : ImportResult
    data object CannotOpen : ImportResult
}

/**
 * Adiciona um livro escolhido pelo SAF: guarda a permissão persistente, lê os metadados e a capa.
 * O arquivo continua onde está; o app só guarda o URI.
 */
@Singleton
class BookImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val readium: Readium,
    private val books: BookRepository,
    private val covers: CoverStore,
) {
    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        books.findByUri(uri.toString())?.let { return@withContext ImportResult.AlreadyInLibrary(it.id) }

        val url = uri.toAbsoluteUrl() ?: return@withContext ImportResult.CannotOpen
        val asset = readium.assetRetriever.retrieve(url).getOrElse { return@withContext ImportResult.Unsupported }
        val publication = readium.publicationOpener.open(asset, allowUserInteraction = false)
            .getOrElse {
                asset.close()
                return@withContext ImportResult.CannotOpen
            }

        try {
            val format = when {
                publication.conformsTo(Publication.Profile.EPUB) -> BookFormat.EPUB
                publication.conformsTo(Publication.Profile.PDF) -> BookFormat.PDF
                else -> return@withContext ImportResult.Unsupported
            }
            val title = publication.metadata.title?.takeIf { it.isNotBlank() } ?: displayName(uri)
            val author = publication.metadata.authors.joinToString(", ") { it.name }.ifBlank { null }
            val coverPath = runCatching { publication.cover() }.getOrNull()?.let { covers.save(it) }

            when (
                val result = books.add(
                    NewBook(
                        uri = uri.toString(),
                        title = title,
                        author = author,
                        format = format,
                        mediaType = asset.format.mediaType.toString(),
                        coverPath = coverPath,
                    ),
                )
            ) {
                is AddBookResult.Added -> ImportResult.Added(result.id, title)
                is AddBookResult.AlreadyInLibrary -> {
                    coverPath?.let { File(it).delete() }
                    ImportResult.AlreadyInLibrary(result.id)
                }
            }
        } finally {
            publication.close()
        }
    }

    private fun displayName(uri: Uri): String {
        val name = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        return name?.substringBeforeLast('.')?.ifBlank { null } ?: uri.lastPathSegment.orEmpty()
    }
}

/** Capas reduzidas guardadas no armazenamento interno do app. */
@Singleton
class CoverStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val dir: File get() = File(context.filesDir, "covers").apply { mkdirs() }

    fun save(bitmap: Bitmap): String? = runCatching {
        val scale = MAX_HEIGHT.toFloat() / bitmap.height
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), MAX_HEIGHT, true)
        } else {
            bitmap
        }
        val file = File(dir, "${System.nanoTime()}.webp")
        file.outputStream().use { scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, 88, it) }
        file.absolutePath
    }.getOrNull()

    fun delete(path: String?) {
        path?.let { File(it).delete() }
    }

    private companion object {
        const val MAX_HEIGHT = 480
    }
}
