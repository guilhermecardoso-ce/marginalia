package dev.kardoso.marginalia.annotations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kardoso.marginalia.core.theme.HighlightColor
import dev.kardoso.marginalia.library.Book
import dev.kardoso.marginalia.library.BookRepository
import dev.kardoso.marginalia.reader.ReaderRepository
import java.time.LocalDate
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

enum class NotesFilter { ALL, HIGHLIGHTS, NOTES }

data class ChapterGroup(val chapter: String?, val items: List<Highlight>)

data class BookNotesUiState(
    val book: Book? = null,
    val filter: NotesFilter = NotesFilter.ALL,
    val groups: List<ChapterGroup> = emptyList(),
    val total: Int = 0,
    val highlightCount: Int = 0,
    val noteCount: Int = 0,
    val loaded: Boolean = false,
)

data class ExportUiState(
    val options: ExportOptions = ExportOptions(),
    val highlightCount: Int = 0,
    val noteCount: Int = 0,
    val markdown: String = "",
    val fileName: String = "anotacoes.md",
    val exportedCount: Int = 0,
)

sealed interface NotesEvent {
    data class OpenReader(val bookId: Long) : NotesEvent
    data object OpenFailed : NotesEvent
}

/** Compartilhado pelas telas "Anotações do livro" e "Exportar". */
@HiltViewModel
class BookNotesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    books: BookRepository,
    private val annotations: AnnotationRepository,
    private val readers: ReaderRepository,
) : ViewModel() {

    val bookId: Long = checkNotNull(savedStateHandle["bookId"])

    private val filter = MutableStateFlow(NotesFilter.ALL)
    private val exportOptions = MutableStateFlow(ExportOptions())
    private val _events = Channel<NotesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val highlights = annotations.highlights(bookId)
    private val book = books.observe(bookId)

    val state: StateFlow<BookNotesUiState> = combine(book, highlights, filter) { book, all, filter ->
        val visible = when (filter) {
            NotesFilter.ALL -> all
            NotesFilter.HIGHLIGHTS -> all.filterNot { it.hasNote }
            NotesFilter.NOTES -> all.filter { it.hasNote }
        }
        BookNotesUiState(
            book = book,
            filter = filter,
            groups = groupByChapter(visible),
            total = all.size,
            highlightCount = all.count { !it.hasNote },
            noteCount = all.count { it.hasNote },
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookNotesUiState())

    val export: StateFlow<ExportUiState> = combine(book, highlights, exportOptions) { book, all, options ->
        val title = book?.title.orEmpty()
        ExportUiState(
            options = options,
            highlightCount = all.count { !it.hasNote },
            noteCount = all.count { it.hasNote },
            markdown = MarkdownExporter.export(title, book?.author, all, options, LocalDate.now()),
            fileName = MarkdownExporter.fileName(title),
            exportedCount = MarkdownExporter.select(all, options).size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExportUiState())

    fun setFilter(value: NotesFilter) = filter.update { value }

    fun setExportOptions(options: ExportOptions) = exportOptions.update { options }

    fun setNote(id: Long, note: String) = viewModelScope.launch { annotations.setNote(id, note) }

    fun setColor(id: Long, color: HighlightColor) = viewModelScope.launch { annotations.setColor(id, color) }

    fun delete(id: Long) = viewModelScope.launch { annotations.deleteHighlight(id) }

    fun openAt(highlight: Highlight) {
        viewModelScope.launch {
            readers.open(bookId, highlight.locatorJson)
                .onSuccess { _events.send(NotesEvent.OpenReader(bookId)) }
                .onFailure { _events.send(NotesEvent.OpenFailed) }
        }
    }

    private fun groupByChapter(items: List<Highlight>): List<ChapterGroup> {
        val groups = mutableListOf<ChapterGroup>()
        for (item in items) {
            val last = groups.lastOrNull()
            if (last != null && last.chapter == item.chapter) {
                groups[groups.lastIndex] = last.copy(items = last.items + item)
            } else {
                groups += ChapterGroup(item.chapter, listOf(item))
            }
        }
        return groups
    }
}
