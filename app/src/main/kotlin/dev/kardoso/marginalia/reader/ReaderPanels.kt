package dev.kardoso.marginalia.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kardoso.marginalia.R
import dev.kardoso.marginalia.annotations.annotationMeta
import dev.kardoso.marginalia.core.theme.ExcerptStyle
import dev.kardoso.marginalia.core.theme.LocalPalette
import dev.kardoso.marginalia.core.theme.MetaStyle
import dev.kardoso.marginalia.core.ui.EmptyState
import dev.kardoso.marginalia.core.ui.Hairline
import dev.kardoso.marginalia.core.ui.IconAction
import dev.kardoso.marginalia.core.ui.SectionLabel
import dev.kardoso.marginalia.library.BookFormat
import kotlin.math.roundToInt

/** Sumário, anotações e marcadores do livro aberto, em tela cheia. */
@Composable
fun ContentsPanel(viewModel: ReaderViewModel, state: ReaderUiState) {
    val palette = LocalPalette.current
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .clickable(enabled = false) {}
            .statusBarsPadding(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            IconAction(R.drawable.ic_close, stringResource(R.string.contents_close), viewModel::closePanel)
            Column(Modifier.weight(1f).padding(end = 16.dp)) {
                Text(state.title, style = MaterialTheme.typography.titleLarge, color = palette.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    if (state.totalPositions > 0) {
                        stringResource(R.string.contents_progress, (state.progression * 100).roundToInt(), state.totalPositions)
                    } else {
                        stringResource(R.string.contents_progress_short, (state.progression * 100).roundToInt())
                    },
                    style = MetaStyle,
                    color = palette.secondary,
                )
            }
        }

        val tabs = buildList {
            add(ContentsTab.TOC to stringResource(R.string.contents_toc))
            if (state.format == BookFormat.EPUB) add(ContentsTab.ANNOTATIONS to stringResource(R.string.contents_annotations, highlights.size))
            add(ContentsTab.BOOKMARKS to stringResource(R.string.contents_bookmarks, bookmarks.size))
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
        ) {
            tabs.forEach { (tab, label) ->
                val selected = tab == state.contentsTab
                Column(
                    Modifier
                        .width(IntrinsicSize.Max)
                        .heightIn(min = 44.dp)
                        .selectable(selected = selected, role = Role.Tab) { viewModel.setContentsTab(tab) },
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
                        color = if (selected) palette.text else palette.secondary,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                    Box(Modifier.fillMaxWidth().height(2.dp).background(if (selected) palette.text else Color.Transparent))
                }
            }
        }
        Hairline()

        Box(Modifier.weight(1f).navigationBarsPadding()) {
            when (state.contentsTab) {
                ContentsTab.TOC -> TocList(state.toc, state.currentTocIndex, viewModel::goTo)
                ContentsTab.ANNOTATIONS -> if (highlights.isEmpty()) {
                    EmptyState(stringResource(R.string.notes_empty_title), stringResource(R.string.reader_notes_empty), Modifier.fillMaxWidth())
                } else {
                    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(highlights, key = { it.id }) { highlight ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.goToJson(highlight.locatorJson) }
                                    .padding(start = 20.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        buildAnnotatedString {
                                            withStyle(SpanStyle(background = highlight.color.on(palette))) { append(highlight.text) }
                                        },
                                        style = ExcerptStyle,
                                        color = palette.text,
                                        maxLines = 6,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    highlight.note?.takeIf { it.isNotBlank() }?.let {
                                        Text(it, style = MaterialTheme.typography.bodyMedium, color = palette.text)
                                    }
                                    Text(annotationMeta(highlight.position, highlight.createdAt), style = MetaStyle, color = palette.secondary)
                                }
                                IconAction(R.drawable.ic_delete, stringResource(R.string.action_delete), { viewModel.deleteHighlight(highlight.id) }, tint = palette.secondary)
                            }
                            Hairline(Modifier.padding(horizontal = 20.dp))
                        }
                    }
                }
                ContentsTab.BOOKMARKS -> if (bookmarks.isEmpty()) {
                    EmptyState(stringResource(R.string.bookmarks_empty_title), stringResource(R.string.bookmarks_empty_body), Modifier.fillMaxWidth())
                } else {
                    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(bookmarks, key = { it.id }) { bookmark ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.goToJson(bookmark.locatorJson) }
                                    .padding(start = 20.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        bookmark.chapter ?: stringResource(R.string.bookmark_untitled),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = palette.text,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(annotationMeta(bookmark.position, bookmark.createdAt), style = MetaStyle, color = palette.secondary)
                                }
                                IconAction(R.drawable.ic_delete, stringResource(R.string.action_delete), { viewModel.deleteBookmark(bookmark.id) }, tint = palette.secondary)
                            }
                            Hairline(Modifier.padding(horizontal = 20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TocList(entries: List<TocEntry>, current: Int, onSelect: (TocEntry) -> Unit) {
    val palette = LocalPalette.current
    if (entries.isEmpty()) {
        EmptyState(stringResource(R.string.toc_empty_title), stringResource(R.string.toc_empty_body), Modifier.fillMaxWidth())
        return
    }
    val listState = rememberLazyListState()
    LaunchedEffect(current) {
        if (current > 2) listState.scrollToItem((current - 2).coerceAtLeast(0))
    }
    LazyColumn(state = listState, contentPadding = PaddingValues(vertical = 8.dp)) {
        itemsIndexed(entries) { index, entry ->
            val isCurrent = index == current
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isCurrent) 8.dp else 0.dp)
                    .heightIn(min = if (entry.depth == 0) 48.dp else 44.dp)
                    .background(if (isCurrent) palette.selected else Color.Transparent, MaterialTheme.shapes.medium)
                    .clickable { onSelect(entry) }
                    .padding(
                        start = (if (isCurrent) 12.dp else 20.dp) + (20 * entry.depth.coerceAtMost(3)).dp,
                        end = if (isCurrent) 12.dp else 20.dp,
                        top = 8.dp,
                        bottom = 8.dp,
                    ),
            ) {
                Text(
                    entry.title,
                    style = (if (entry.depth == 0) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium)
                        .copy(fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal),
                    color = palette.text,
                    modifier = Modifier.weight(1f),
                )
                entry.position?.let {
                    Text(it.toString(), style = MaterialTheme.typography.bodySmall, color = palette.secondary)
                }
            }
        }
    }
}

/** Busca no livro: resultados com o trecho em volta, toque leva ao ponto. */
@Composable
fun SearchPanel(viewModel: ReaderViewModel, search: SearchUi) {
    val palette = LocalPalette.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .clickable(enabled = false) {}
            .statusBarsPadding()
            .imePadding(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            IconAction(R.drawable.ic_arrow_back, stringResource(R.string.action_close_search), viewModel::closePanel)
            TextField(
                value = search.query,
                onValueChange = viewModel::setSearchQuery,
                singleLine = true,
                placeholder = { Text(stringResource(R.string.reader_search_hint), color = palette.secondary) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.search(search.query) }),
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = palette.line,
                    unfocusedIndicatorColor = palette.line,
                    cursorColor = palette.brand,
                    focusedTextColor = palette.text,
                    unfocusedTextColor = palette.text,
                ),
                modifier = Modifier.weight(1f).padding(end = 12.dp).focusRequester(focus),
            )
        }
        if (search.running) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = palette.brand,
                trackColor = Color.Transparent,
            )
        }
        if (search.searched || search.hits.isNotEmpty()) {
            SectionLabel(
                pluralStringResource(R.plurals.search_results, search.hits.size, search.hits.size),
                Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
            )
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.navigationBarsPadding()) {
            itemsIndexed(search.hits) { _, hit ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.goTo(hit.locator) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    hit.chapter?.let { Text(it, style = MetaStyle, color = palette.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    Text(
                        buildAnnotatedString {
                            append(hit.before)
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, background = palette.brand.copy(alpha = 0.18f))) {
                                append(hit.match)
                            }
                            append(hit.after)
                        },
                        style = ExcerptStyle.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize),
                        color = palette.text,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Hairline(Modifier.padding(horizontal = 20.dp))
            }
        }
    }
}
