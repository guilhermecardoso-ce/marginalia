package dev.kardoso.marginalia.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kardoso.marginalia.R
import dev.kardoso.marginalia.core.theme.LocalPalette
import dev.kardoso.marginalia.core.theme.MetaStyle
import dev.kardoso.marginalia.core.ui.BookCover
import dev.kardoso.marginalia.core.ui.EmptyState
import dev.kardoso.marginalia.core.ui.Hairline
import dev.kardoso.marginalia.core.ui.IconAction
import dev.kardoso.marginalia.core.ui.PrimaryButton
import dev.kardoso.marginalia.core.ui.ProgressLine
import dev.kardoso.marginalia.core.ui.SectionLabel
import kotlin.math.roundToInt

private val BookMimeTypes = arrayOf("application/epub+zip", "application/pdf")

@Composable
fun LibraryScreen(
    contentPadding: PaddingValues,
    onOpenReader: (Long) -> Unit,
    showMessage: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.import(uris)
    }
    val addBook = { picker.launch(BookMimeTypes) }

    val importedText = stringResource(R.string.library_imported)
    val alreadyText = stringResource(R.string.library_already)
    val importFailedText = stringResource(R.string.library_import_failed)
    val missingText = stringResource(R.string.library_file_missing)
    val openFailedText = stringResource(R.string.library_open_failed)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is LibraryEvent.OpenReader -> onOpenReader(event.bookId)
                is LibraryEvent.Imported -> showMessage(importedText.format(event.title))
                LibraryEvent.AlreadyInLibrary -> showMessage(alreadyText)
                LibraryEvent.ImportFailed -> showMessage(importFailedText)
                LibraryEvent.FileMissing -> showMessage(missingText)
                LibraryEvent.OpenFailed -> showMessage(openFailedText)
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        if (state.searching) {
            SearchBar(query = state.query, onQuery = viewModel::setQuery, onClose = { viewModel.setSearching(false) })
        } else {
            LibraryHeader(onSearch = { viewModel.setSearching(true) }, onAdd = addBook)
        }
        if (state.importing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = LocalPalette.current.brand,
                trackColor = Color.Transparent,
            )
        }

        when {
            !state.loaded -> Unit
            state.totalCount == 0 -> EmptyState(
                title = stringResource(R.string.library_empty_title),
                body = stringResource(R.string.library_empty_body),
                modifier = Modifier.fillMaxWidth(),
            ) {
                PrimaryButton(stringResource(R.string.library_add), onClick = addBook)
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                state.continueReading?.let { book ->
                    item(key = "continue") {
                        ContinueReading(book = book, onOpen = { viewModel.open(book.id) })
                    }
                }
                item(key = "header") {
                    AllBooksHeader(
                        count = state.books.size,
                        sort = state.sort,
                        onSort = viewModel::setSort,
                        topPadding = if (state.continueReading != null) 28.dp else 12.dp,
                    )
                }
                if (state.books.isEmpty()) {
                    item(key = "no-results") {
                        Text(
                            stringResource(R.string.library_no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalPalette.current.secondary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        )
                    }
                }
                items(state.books, key = { it.id }) { book ->
                    BookRow(book = book, onOpen = { viewModel.open(book.id) }, onRemove = { viewModel.remove(book) })
                    Hairline(Modifier.padding(horizontal = 20.dp))
                }
            }
        }
    }
}

@Composable
private fun LibraryHeader(onSearch: () -> Unit, onAdd: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
    ) {
        Text(
            stringResource(R.string.library_title),
            style = MaterialTheme.typography.headlineMedium,
            color = LocalPalette.current.text,
            modifier = Modifier.weight(1f),
        )
        IconAction(R.drawable.ic_search, stringResource(R.string.library_search), onSearch)
        IconAction(R.drawable.ic_add, stringResource(R.string.library_add), onAdd)
    }
}

@Composable
private fun SearchBar(query: String, onQuery: (String) -> Unit, onClose: () -> Unit) {
    val palette = LocalPalette.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 4.dp),
    ) {
        IconAction(R.drawable.ic_arrow_back, stringResource(R.string.action_close_search), onClose)
        TextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            placeholder = { Text(stringResource(R.string.library_search_hint), color = palette.secondary) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
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
            modifier = Modifier.weight(1f).focusRequester(focus),
        )
        if (query.isNotEmpty()) {
            IconAction(R.drawable.ic_close, stringResource(R.string.action_clear), { onQuery("") })
        }
    }
}

@Composable
private fun ContinueReading(book: Book, onOpen: () -> Unit) {
    val palette = LocalPalette.current
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
    ) {
        SectionLabel(stringResource(R.string.library_continue))
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.line, MaterialTheme.shapes.medium)
                .padding(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BookCover(book.title, book.coverPath, width = 72.dp, height = 108.dp)
                Column(Modifier.weight(1f).heightIn(min = 108.dp)) {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.text,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    book.author?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = palette.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.weight(1f).heightIn(min = 8.dp))
                    ProgressLine(book.progression.toFloat())
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.library_percent, (book.progression * 100).roundToInt()),
                        style = MetaStyle,
                        color = palette.secondary,
                    )
                }
            }
            PrimaryButton(stringResource(R.string.library_continue_action), onClick = onOpen, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun AllBooksHeader(count: Int, sort: LibrarySort, onSort: (LibrarySort) -> Unit, topPadding: Dp) {
    var expanded by remember { mutableStateOf(false) }
    val palette = LocalPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = topPadding, bottom = 4.dp),
    ) {
        SectionLabel(pluralStringResource(R.plurals.library_all_books, count, count), Modifier.weight(1f))
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(stringResource(sort.label()), style = MaterialTheme.typography.labelMedium, color = palette.text)
                Icon(painterResource(R.drawable.ic_expand_more), contentDescription = null, tint = palette.text, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, shape = MaterialTheme.shapes.medium) {
                LibrarySort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option.label()), style = MaterialTheme.typography.bodyLarge) },
                        onClick = {
                            onSort(option)
                            expanded = false
                        },
                        trailingIcon = if (option == sort) {
                            { Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(20.dp)) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookRow(book: Book, onOpen: () -> Unit, onRemove: () -> Unit) {
    val palette = LocalPalette.current
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onOpen, onLongClick = { menu = true })
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            BookCover(book.title, book.coverPath, width = 48.dp, height = 72.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, color = palette.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                book.author?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = palette.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(book.statusText(), style = MetaStyle, color = palette.secondary)
                    if (book.format == BookFormat.PDF) {
                        Text(
                            "PDF",
                            style = MetaStyle.copy(fontWeight = FontWeight.SemiBold),
                            color = palette.text,
                            modifier = Modifier
                                .border(1.dp, palette.lineStrong, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = MaterialTheme.shapes.medium) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.library_remove), style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
                onClick = {
                    menu = false
                    onRemove()
                },
            )
        }
    }
}

@Composable
private fun Book.statusText(): String = when {
    isFinished -> stringResource(R.string.library_status_finished)
    !isStarted -> stringResource(R.string.library_status_new)
    else -> stringResource(R.string.library_percent, (progression * 100).roundToInt().coerceAtLeast(1))
}

private fun LibrarySort.label(): Int = when (this) {
    LibrarySort.RECENT -> R.string.sort_recent
    LibrarySort.TITLE -> R.string.sort_title
    LibrarySort.AUTHOR -> R.string.sort_author
}
