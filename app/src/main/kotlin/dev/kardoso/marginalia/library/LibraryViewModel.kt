package dev.kardoso.marginalia.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kardoso.marginalia.reader.OpenError
import dev.kardoso.marginalia.reader.OpenException
import dev.kardoso.marginalia.reader.ReaderRepository
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer

enum class LibrarySort { RECENT, TITLE, AUTHOR }

data class LibraryUiState(
    val loaded: Boolean = false,
    val continueReading: Book? = null,
    val books: List<Book> = emptyList(),
    val totalCount: Int = 0,
    val sort: LibrarySort = LibrarySort.RECENT,
    val query: String = "",
    val searching: Boolean = false,
    val importing: Boolean = false,
)

sealed interface LibraryEvent {
    data class OpenReader(val bookId: Long) : LibraryEvent
    data class Imported(val title: String) : LibraryEvent
    data object AlreadyInLibrary : LibraryEvent
    data object ImportFailed : LibraryEvent
    data object FileMissing : LibraryEvent
    data object OpenFailed : LibraryEvent
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val books: BookRepository,
    private val importer: BookImporter,
    private val covers: CoverStore,
    private val readers: ReaderRepository,
) : ViewModel() {

    private val controls = MutableStateFlow(LibraryUiState())
    private val _events = Channel<LibraryEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val state: StateFlow<LibraryUiState> = combine(books.books, controls) { all, controls ->
        val continueReading = all
            .filter { it.lastOpenedAt != null && it.isStarted && !it.isFinished }
            .maxByOrNull { it.lastOpenedAt ?: 0L }
        val filtered = all.filter { it.matches(controls.query) }
        controls.copy(
            loaded = true,
            continueReading = continueReading.takeIf { controls.query.isBlank() },
            books = filtered.sortedWith(controls.sort.comparator()),
            totalCount = all.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun setSort(sort: LibrarySort) = controls.update { it.copy(sort = sort) }

    fun setSearching(searching: Boolean) = controls.update { it.copy(searching = searching, query = if (searching) it.query else "") }

    fun setQuery(query: String) = controls.update { it.copy(query = query) }

    fun import(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            controls.update { it.copy(importing = true) }
            for (uri in uris) {
                val event = when (val result = importer.import(uri)) {
                    is ImportResult.Added -> LibraryEvent.Imported(result.title)
                    is ImportResult.AlreadyInLibrary -> LibraryEvent.AlreadyInLibrary
                    ImportResult.CannotOpen, ImportResult.Unsupported -> LibraryEvent.ImportFailed
                }
                _events.send(event)
            }
            controls.update { it.copy(importing = false) }
        }
    }

    fun open(bookId: Long) {
        viewModelScope.launch {
            readers.open(bookId)
                .onSuccess { _events.send(LibraryEvent.OpenReader(bookId)) }
                .onFailure { error ->
                    val event = when ((error as? OpenException)?.error) {
                        OpenError.FileMissing -> LibraryEvent.FileMissing
                        else -> LibraryEvent.OpenFailed
                    }
                    _events.send(event)
                }
        }
    }

    fun remove(book: Book) {
        viewModelScope.launch {
            readers.close(book.id)
            books.delete(book.id)
            covers.delete(book.coverPath)
        }
    }
}

private fun LibrarySort.comparator(): Comparator<Book> = when (this) {
    LibrarySort.RECENT -> compareByDescending { it.lastOpenedAt ?: it.addedAt }
    LibrarySort.TITLE -> compareBy { it.title.sortKey() }
    LibrarySort.AUTHOR -> compareBy<Book> { it.author?.sortKey() ?: "￿" }.thenBy { it.title.sortKey() }
}

private fun Book.matches(query: String): Boolean {
    if (query.isBlank()) return true
    val needle = query.sortKey()
    return title.sortKey().contains(needle) || (author?.sortKey()?.contains(needle) ?: false)
}

private fun String.sortKey(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase().trim()
