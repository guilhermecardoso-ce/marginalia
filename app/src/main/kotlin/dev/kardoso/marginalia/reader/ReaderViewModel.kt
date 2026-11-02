package dev.kardoso.marginalia.reader

import android.graphics.RectF
import android.os.SystemClock
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kardoso.marginalia.annotations.AnnotationRepository
import dev.kardoso.marginalia.annotations.BookPoint
import dev.kardoso.marginalia.annotations.Bookmark
import dev.kardoso.marginalia.annotations.Highlight
import dev.kardoso.marginalia.core.prefs.PreferencesRepository
import dev.kardoso.marginalia.core.prefs.ReadingPreferences
import dev.kardoso.marginalia.core.readium.locatorFromJson
import dev.kardoso.marginalia.core.readium.toJsonString
import dev.kardoso.marginalia.core.theme.HighlightColor
import dev.kardoso.marginalia.library.BookFormat
import dev.kardoso.marginalia.library.BookRepository
import javax.inject.Inject
import kotlin.math.ceil
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.HyperlinkNavigator
import org.readium.r2.navigator.Selection
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.search.isSearchable
import org.readium.r2.shared.publication.services.locateProgression
import org.readium.r2.shared.publication.services.positionsByReadingOrder
import org.readium.r2.shared.publication.services.search.search
import org.readium.r2.shared.util.AbsoluteUrl

enum class ReaderPanel { NONE, CONTENTS, APPEARANCE, SEARCH }

enum class ContentsTab { TOC, ANNOTATIONS, BOOKMARKS }

data class TocEntry(val title: String, val depth: Int, val link: Link, val href: String, val position: Int?) {
    val fragment: String? get() = link.url().toString().substringAfter('#', "").ifEmpty { null }
}

/** Menu de seleção: um trecho novo (sem `highlightId`) ou um destaque existente. */
data class SelectionUi(
    val rect: RectF?,
    val text: String,
    val locator: Locator?,
    val highlightId: Long? = null,
    val color: HighlightColor? = null,
)

data class FootnoteUi(val content: String, val link: Link)

data class NoteEditorUi(val excerpt: String, val initial: String?, val highlightId: Long?, val locator: Locator?)

data class SearchHit(val locator: Locator, val before: String, val match: String, val after: String, val chapter: String?)

data class SearchUi(
    val query: String = "",
    val running: Boolean = false,
    val searched: Boolean = false,
    val hits: List<SearchHit> = emptyList(),
)

data class ReaderUiState(
    val title: String = "",
    val format: BookFormat = BookFormat.EPUB,
    val searchable: Boolean = false,
    val barsVisible: Boolean = false,
    val chapter: String? = null,
    val progression: Float = 0f,
    val position: Int? = null,
    val totalPositions: Int = 0,
    val minutesLeftInChapter: Int? = null,
    val panel: ReaderPanel = ReaderPanel.NONE,
    val contentsTab: ContentsTab = ContentsTab.TOC,
    val toc: List<TocEntry> = emptyList(),
    val currentTocIndex: Int = -1,
    val selection: SelectionUi? = null,
    val footnote: FootnoteUi? = null,
    val noteEditor: NoteEditorUi? = null,
    val search: SearchUi = SearchUi(),
)

sealed interface ReaderCommand {
    data class Go(val locator: Locator) : ReaderCommand
    data class GoToLink(val link: Link) : ReaderCommand
    data class Copy(val text: String) : ReaderCommand
    data class OpenExternal(val url: AbsoluteUrl) : ReaderCommand
    data object ClearSelection : ReaderCommand
    data object Finish : ReaderCommand
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val readers: ReaderRepository,
    private val books: BookRepository,
    private val annotations: AnnotationRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel(), EpubNavigatorFragment.Listener, PdfNavigatorFragment.Listener, DecorableNavigator.Listener {

    val bookId: Long = savedStateHandle[ReaderActivity.EXTRA_BOOK_ID] ?: -1L
    private val session: ReaderSession? = readers[bookId]
    val publication: Publication? get() = session?.publication

    private val _state = MutableStateFlow(
        ReaderUiState(
            title = session?.book?.title.orEmpty(),
            format = session?.format ?: BookFormat.EPUB,
            searchable = session?.publication?.isSearchable == true,
        ),
    )
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    private val _commands = Channel<ReaderCommand>(Channel.BUFFERED)
    val commands: Flow<ReaderCommand> = _commands.receiveAsFlow()

    val preferences: StateFlow<ReadingPreferences> = preferencesRepository.reading
        .stateIn(viewModelScope, SharingStarted.Eagerly, session?.preferences ?: ReadingPreferences())

    val highlights: StateFlow<List<Highlight>> =
        (if (session != null) annotations.highlights(bookId) else flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val bookmarks: StateFlow<List<Bookmark>> =
        (if (session != null) annotations.bookmarks(bookId) else flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val currentPoint = MutableStateFlow<BookPoint?>(null)

    val bookmarked: StateFlow<Boolean> = combine(currentPoint, bookmarks) { point, list ->
        point != null && AnnotationRepository.findBookmarkAt(point, list) != null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val activeHighlightId = MutableStateFlow<Long?>(null)

    /** Destaques convertidos em decorações do Readium, na cor do tema ativo. */
    val decorations: Flow<List<Decoration>> =
        if (session?.format != BookFormat.EPUB) {
            emptyFlow()
        } else {
            combine(highlights, preferences, activeHighlightId) { list, prefs, active ->
                list.mapNotNull { highlight ->
                    val locator = locatorFromJson(highlight.locatorJson) ?: return@mapNotNull null
                    Decoration(
                        id = "highlight-${highlight.id}",
                        locator = locator,
                        style = Decoration.Style.Highlight(
                            tint = highlight.color.on(prefs.theme.palette).toArgb(),
                            isActive = highlight.id == active,
                        ),
                        extras = mapOf(HIGHLIGHT_ID to highlight.id),
                    )
                }
            }
        }

    private var positionsByResource: List<List<Locator>> = emptyList()
    private val initialLocator: Locator? = session?.lastLocator
    private val openedAt = SystemClock.elapsedRealtime()
    private var restored = false
    private var searchJob: Job? = null

    init {
        if (session == null) {
            viewModelScope.launch { _commands.send(ReaderCommand.Finish) }
        } else {
            viewModelScope.launch { loadStructure(session.publication) }
        }
    }

    private suspend fun loadStructure(publication: Publication) {
        positionsByResource = runCatching { publication.positionsByReadingOrder() }.getOrDefault(emptyList())
        val firstPositionByHref = buildMap {
            positionsByResource.forEach { list ->
                list.firstOrNull()?.let { put(it.href.toString().baseHref(), it.locations.position) }
            }
        }
        val toc = buildList {
            fun walk(links: List<Link>, depth: Int) {
                links.forEach { link ->
                    val href = link.url().toString()
                    add(
                        TocEntry(
                            title = link.title?.trim().orEmpty().ifEmpty { href.baseHref().substringAfterLast('/') },
                            depth = depth,
                            link = link,
                            href = href.baseHref(),
                            position = firstPositionByHref[href.baseHref()],
                        ),
                    )
                    walk(link.children, depth + 1)
                }
            }
            walk(publication.tableOfContents, 0)
        }
        _state.update { it.copy(toc = toc, totalPositions = positionsByResource.sumOf { list -> list.size }) }
        session?.lastLocator?.let { updateLocation(it) }
    }

    // Posição de leitura

    fun onLocatorChanged(locator: Locator) {
        val session = session ?: return
        if (!restored) {
            // Enquanto o navegador carrega, ele pode emitir outro recurso antes de chegar à posição
            // salva. Essas emissões não podem sobrescrever o progresso.
            val target = initialLocator
            val reached = target == null ||
                locator.href.toString().substringBefore('#') == target.href.toString().substringBefore('#')
            if (!reached && SystemClock.elapsedRealtime() - openedAt < RESTORE_WINDOW_MS) return
            restored = true
        }
        session.lastLocator = locator
        updateLocation(locator)
        // Guarda o capítulo calculado quando o livro não traz título no locator (cartão "Continuar lendo").
        val saved = if (locator.title.isNullOrBlank()) locator.copy(title = _state.value.chapter) else locator
        viewModelScope.launch {
            books.saveProgress(bookId, saved.toJsonString(), locator.locations.totalProgression)
        }
    }

    private fun updateLocation(locator: Locator) {
        val position = locator.locations.position
        val base = locator.href.toString().baseHref()
        val toc = _state.value.toc
        // Subseções do mesmo arquivo só ganham do capítulo quando o locator aponta para a âncora delas.
        val fragments = locator.locations.fragments.toSet()
        val tocIndex = toc.indexOfFirst { it.href == base && it.fragment != null && it.fragment in fragments }
            .takeIf { it >= 0 }
            ?: toc.indexOfFirst { it.href == base }.takeIf { it >= 0 }
            ?: toc.indexOfLast { it.position != null && position != null && it.position <= position }
        val chapter = locator.title?.trim()?.ifEmpty { null } ?: toc.getOrNull(tocIndex)?.title
        // Só estima o tempo quando o arquivo corresponde a um único capítulo.
        val chaptersInResource = toc.count { it.href == base && it.depth == toc.getOrNull(tocIndex)?.depth }
        val minutes = if (_state.value.format == BookFormat.EPUB && position != null && chaptersInResource <= 1) {
            positionsByResource.firstOrNull { list -> list.any { it.href.toString().baseHref() == base } }
                ?.lastOrNull()?.locations?.position
                ?.let { last -> ceil((last - position + 1) * MINUTES_PER_POSITION).toInt().coerceAtLeast(1) }
        } else {
            null
        }
        _state.update {
            it.copy(
                chapter = chapter,
                progression = (locator.locations.totalProgression ?: 0.0).toFloat(),
                position = position,
                currentTocIndex = tocIndex,
                minutesLeftInChapter = minutes,
            )
        }
        currentPoint.value = BookPoint(
            locatorJson = locator.toJsonString(),
            chapter = chapter,
            position = position,
            progression = locator.locations.totalProgression ?: 0.0,
        )
    }

    fun seekTo(progression: Float) {
        val publication = publication ?: return
        viewModelScope.launch {
            publication.locateProgression(progression.toDouble().coerceIn(0.0, 1.0))?.let {
                _commands.send(ReaderCommand.Go(it))
            }
        }
    }

    // Barras e painéis

    fun onCenterTap() {
        if (_state.value.selection != null) {
            dismissSelection()
            return
        }
        _state.update { it.copy(barsVisible = !it.barsVisible) }
    }

    fun hideBars() = _state.update { it.copy(barsVisible = false) }

    fun openContents(tab: ContentsTab = ContentsTab.TOC) =
        _state.update { it.copy(panel = ReaderPanel.CONTENTS, contentsTab = tab) }

    fun setContentsTab(tab: ContentsTab) = _state.update { it.copy(contentsTab = tab) }

    fun openAppearance() = _state.update { it.copy(panel = ReaderPanel.APPEARANCE) }

    fun openSearch() = _state.update { it.copy(panel = ReaderPanel.SEARCH) }

    fun closePanel() = _state.update { it.copy(panel = ReaderPanel.NONE) }

    /** Voltar: fecha o que estiver aberto antes de sair do livro. */
    fun onBack(): Boolean {
        val current = _state.value
        when {
            current.noteEditor != null -> _state.update { it.copy(noteEditor = null) }
            current.footnote != null -> _state.update { it.copy(footnote = null) }
            current.selection != null -> dismissSelection()
            current.panel != ReaderPanel.NONE -> closePanel()
            else -> return false
        }
        return true
    }

    fun goTo(locator: Locator) {
        _state.update { it.copy(panel = ReaderPanel.NONE, barsVisible = false) }
        viewModelScope.launch { _commands.send(ReaderCommand.Go(locator)) }
    }

    fun goTo(entry: TocEntry) {
        _state.update { it.copy(panel = ReaderPanel.NONE, barsVisible = false) }
        viewModelScope.launch { _commands.send(ReaderCommand.GoToLink(entry.link)) }
    }

    fun goToJson(locatorJson: String) {
        locatorFromJson(locatorJson)?.let { goTo(it) }
    }

    fun updatePreferences(next: ReadingPreferences) {
        session?.preferences = next
        viewModelScope.launch { preferencesRepository.update { next } }
    }

    fun resetPreferences() {
        viewModelScope.launch { preferencesRepository.reset() }
    }

    // Marcadores

    fun toggleBookmark() {
        val point = currentPoint.value ?: return
        viewModelScope.launch { annotations.toggleBookmark(bookId, point, bookmarks.value) }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch { annotations.deleteBookmark(id) }
    }

    // Seleção e destaques

    fun onSelection(selection: Selection?) {
        selection ?: return
        val text = selection.locator.text.highlight?.trim().orEmpty()
        if (text.isEmpty()) return
        _state.update {
            it.copy(selection = SelectionUi(rect = selection.rect, text = text, locator = selection.locator), barsVisible = false)
        }
    }

    fun onSelectionCleared() {
        if (_state.value.selection?.highlightId == null) {
            _state.update { it.copy(selection = null) }
        }
    }

    fun dismissSelection() {
        activeHighlightId.value = null
        _state.update { it.copy(selection = null) }
        viewModelScope.launch { _commands.send(ReaderCommand.ClearSelection) }
    }

    fun highlight(color: HighlightColor) {
        val selection = _state.value.selection ?: return
        viewModelScope.launch {
            if (selection.highlightId != null) {
                annotations.setColor(selection.highlightId, color)
            } else {
                val locator = selection.locator ?: return@launch
                annotations.addHighlight(bookId, pointFor(locator), selection.text, color)
            }
        }
        dismissSelection()
    }

    fun editNote() {
        val selection = _state.value.selection ?: return
        viewModelScope.launch {
            val existing = selection.highlightId?.let { annotations.highlight(it) }
            _state.update {
                it.copy(
                    noteEditor = NoteEditorUi(
                        excerpt = selection.text,
                        initial = existing?.note,
                        highlightId = selection.highlightId,
                        locator = selection.locator,
                    ),
                )
            }
            dismissSelection()
        }
    }

    fun saveNote(note: String) {
        val editor = _state.value.noteEditor ?: return
        _state.update { it.copy(noteEditor = null) }
        viewModelScope.launch {
            if (editor.highlightId != null) {
                annotations.setNote(editor.highlightId, note)
            } else if (editor.locator != null) {
                annotations.addHighlight(bookId, pointFor(editor.locator), editor.excerpt, HighlightColor.AMARELO, note)
            }
        }
    }

    fun dismissNoteEditor() = _state.update { it.copy(noteEditor = null) }

    fun copySelection() {
        val selection = _state.value.selection ?: return
        viewModelScope.launch { _commands.send(ReaderCommand.Copy(selection.text)) }
        dismissSelection()
    }

    fun deleteSelectedHighlight() {
        val id = _state.value.selection?.highlightId ?: return
        viewModelScope.launch { annotations.deleteHighlight(id) }
        dismissSelection()
    }

    fun deleteHighlight(id: Long) {
        viewModelScope.launch { annotations.deleteHighlight(id) }
    }

    private fun pointFor(locator: Locator) = BookPoint(
        locatorJson = locator.toJsonString(),
        chapter = locator.title?.trim()?.ifEmpty { null } ?: _state.value.chapter,
        position = locator.locations.position ?: _state.value.position,
        progression = locator.locations.totalProgression ?: _state.value.progression.toDouble(),
    )

    override fun onDecorationActivated(event: DecorableNavigator.OnActivatedEvent): Boolean {
        val id = event.decoration.extras[HIGHLIGHT_ID] as? Long ?: (event.decoration.extras[HIGHLIGHT_ID] as? Number)?.toLong()
            ?: return false
        val highlight = highlights.value.firstOrNull { it.id == id } ?: return false
        activeHighlightId.value = id
        _state.update {
            it.copy(
                selection = SelectionUi(
                    rect = event.rect,
                    text = highlight.text,
                    locator = locatorFromJson(highlight.locatorJson),
                    highlightId = id,
                    color = highlight.color,
                ),
                barsVisible = false,
            )
        }
        return true
    }

    // Notas de rodapé e links

    override fun shouldFollowInternalLink(link: Link, context: HyperlinkNavigator.LinkContext?): Boolean {
        if (context is HyperlinkNavigator.FootnoteContext) {
            _state.update { it.copy(footnote = FootnoteUi(context.noteContent, link), barsVisible = false) }
            return false
        }
        return true
    }

    override fun onExternalLinkActivated(url: AbsoluteUrl) {
        viewModelScope.launch { _commands.send(ReaderCommand.OpenExternal(url)) }
    }

    fun dismissFootnote() = _state.update { it.copy(footnote = null) }

    fun goToFootnote() {
        val link = _state.value.footnote?.link ?: return
        _state.update { it.copy(footnote = null) }
        viewModelScope.launch { _commands.send(ReaderCommand.GoToLink(link)) }
    }

    // Busca

    fun search(query: String) {
        val publication = publication ?: return
        val trimmed = query.trim()
        searchJob?.cancel()
        if (trimmed.length < 2) {
            _state.update { it.copy(search = SearchUi(query = query)) }
            return
        }
        _state.update { it.copy(search = SearchUi(query = query, running = true)) }
        searchJob = viewModelScope.launch {
            val iterator = publication.search(trimmed)
            if (iterator == null) {
                _state.update { it.copy(search = it.search.copy(running = false, searched = true)) }
                return@launch
            }
            try {
                while (_state.value.search.hits.size < MAX_SEARCH_HITS) {
                    val page = iterator.next().getOrNull() ?: break
                    val hits = page.locators.map { locator ->
                        SearchHit(
                            locator = locator,
                            before = locator.text.before.orEmpty().takeLast(60).trimStart(),
                            match = locator.text.highlight.orEmpty(),
                            after = locator.text.after.orEmpty().take(80).trimEnd(),
                            chapter = locator.title,
                        )
                    }
                    _state.update { it.copy(search = it.search.copy(hits = it.search.hits + hits)) }
                }
            } finally {
                iterator.close()
                _state.update { it.copy(search = it.search.copy(running = false, searched = true)) }
            }
        }
    }

    fun setSearchQuery(query: String) = _state.update { it.copy(search = it.search.copy(query = query)) }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }

    fun close() {
        readers.close(bookId)
    }

    companion object {
        const val HIGHLIGHT_ID = "id"
        private const val MAX_SEARCH_HITS = 300
        private const val RESTORE_WINDOW_MS = 3_000L

        /** Uma posição do Readium tem ~1024 caracteres: cerca de 0,75 min de leitura. */
        private const val MINUTES_PER_POSITION = 0.75
    }
}

private fun String.baseHref(): String = substringBefore('#')
