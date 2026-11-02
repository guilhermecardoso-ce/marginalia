package dev.kardoso.marginalia.annotations

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kardoso.marginalia.R
import dev.kardoso.marginalia.core.theme.ExcerptStyle
import dev.kardoso.marginalia.core.theme.HighlightColor
import dev.kardoso.marginalia.core.theme.LocalPalette
import dev.kardoso.marginalia.core.theme.MetaStyle
import dev.kardoso.marginalia.core.ui.EmptyState
import dev.kardoso.marginalia.core.ui.FilterChip
import dev.kardoso.marginalia.core.ui.Hairline
import dev.kardoso.marginalia.core.ui.IconAction
import dev.kardoso.marginalia.core.ui.PrimaryButton
import dev.kardoso.marginalia.core.ui.SectionLabel

@Composable
fun BookNotesScreen(
    onBack: () -> Unit,
    onExport: () -> Unit,
    onOpenReader: (Long) -> Unit,
    showMessage: (String) -> Unit,
    viewModel: BookNotesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val palette = LocalPalette.current
    var editing by remember { mutableStateOf<Highlight?>(null) }

    val openFailed = stringResource(R.string.library_open_failed)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is NotesEvent.OpenReader -> onOpenReader(event.bookId)
                NotesEvent.OpenFailed -> showMessage(openFailed)
            }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
            IconAction(R.drawable.ic_arrow_back, stringResource(R.string.action_back), onBack)
        }
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.annotations_title), style = MaterialTheme.typography.headlineMedium, color = palette.text)
            state.book?.let {
                Text(
                    pluralStringResource(R.plurals.notes_subtitle, state.total, it.title, state.total),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.secondary,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
        ) {
            FilterChip(stringResource(R.string.filter_all), state.filter == NotesFilter.ALL) { viewModel.setFilter(NotesFilter.ALL) }
            FilterChip(stringResource(R.string.filter_highlights, state.highlightCount), state.filter == NotesFilter.HIGHLIGHTS) {
                viewModel.setFilter(NotesFilter.HIGHLIGHTS)
            }
            FilterChip(stringResource(R.string.filter_notes, state.noteCount), state.filter == NotesFilter.NOTES) {
                viewModel.setFilter(NotesFilter.NOTES)
            }
        }

        Box(Modifier.weight(1f)) {
            if (state.loaded && state.groups.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.notes_empty_title),
                    body = stringResource(R.string.notes_empty_body),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                state.groups.forEach { group ->
                    item(key = "chapter-${group.items.first().id}") {
                        SectionLabel(
                            group.chapter ?: stringResource(R.string.notes_no_chapter),
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
                        )
                    }
                    items(group.items, key = { it.id }) { highlight ->
                        HighlightItem(
                            highlight = highlight,
                            onOpen = { viewModel.openAt(highlight) },
                            onEditNote = { editing = highlight },
                            onColor = { viewModel.setColor(highlight.id, it) },
                            onDelete = { viewModel.delete(highlight.id) },
                        )
                    }
                }
            }
        }

        if (state.total > 0) {
            Hairline()
            PrimaryButton(
                text = stringResource(R.string.notes_export),
                onClick = onExport,
                icon = R.drawable.ic_download,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
            )
        }
    }

    editing?.let { highlight ->
        NoteEditorDialog(
            excerpt = highlight.text,
            initial = highlight.note,
            onSave = {
                viewModel.setNote(highlight.id, it)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun HighlightItem(
    highlight: Highlight,
    onOpen: () -> Unit,
    onEditNote: () -> Unit,
    onColor: (HighlightColor) -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalPalette.current
    var menu by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(start = 20.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val tint = highlight.color.on(palette)
                Text(
                    buildAnnotatedString { withStyle(SpanStyle(background = tint)) { append(highlight.text) } },
                    style = ExcerptStyle,
                    color = palette.text,
                )
                if (highlight.hasNote) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            painterResource(R.drawable.ic_edit_note),
                            contentDescription = null,
                            tint = palette.secondary,
                            modifier = Modifier.size(18.dp).padding(top = 2.dp),
                        )
                        Text(highlight.note.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = palette.text)
                    }
                }
                Text(annotationMeta(highlight.position, highlight.createdAt), style = MetaStyle, color = palette.secondary)
            }
            Box {
                IconAction(R.drawable.ic_more_vert, stringResource(R.string.action_more), { menu = true }, tint = palette.secondary)
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = MaterialTheme.shapes.medium) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        HighlightColor.entries.forEach { color ->
                            ColorDot(color = color, selected = color == highlight.color) {
                                onColor(color)
                                menu = false
                            }
                        }
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(if (highlight.hasNote) R.string.note_edit else R.string.note_add),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_edit), contentDescription = null) },
                        onClick = {
                            menu = false
                            onEditNote()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete), style = MaterialTheme.typography.bodyLarge) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
                        onClick = {
                            menu = false
                            onDelete()
                        },
                    )
                }
            }
        }
        Hairline(Modifier.padding(horizontal = 20.dp))
    }
}

/** Bolinha de cor de destaque com 44 dp de área de toque. */
@Composable
fun ColorDot(color: HighlightColor, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val name = stringResource(color.label())
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClickLabel = name, onClick = onClick),
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(color.on(palette))
                .border(if (selected) 2.dp else 1.dp, if (selected) palette.text else palette.lineStrong, CircleShape),
        )
    }
}

fun HighlightColor.label(): Int = when (this) {
    HighlightColor.AMARELO -> R.string.color_yellow
    HighlightColor.VERDE -> R.string.color_green
    HighlightColor.AZUL -> R.string.color_blue
    HighlightColor.ROSA -> R.string.color_pink
}
