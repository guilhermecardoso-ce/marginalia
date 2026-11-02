package dev.kardoso.marginalia.reader

import android.graphics.RectF
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kardoso.marginalia.R
import dev.kardoso.marginalia.annotations.ColorDot
import dev.kardoso.marginalia.annotations.NoteEditorDialog
import dev.kardoso.marginalia.core.theme.ExcerptStyle
import dev.kardoso.marginalia.core.theme.HighlightColor
import dev.kardoso.marginalia.core.theme.Literata
import dev.kardoso.marginalia.core.theme.LocalMotionMillis
import dev.kardoso.marginalia.core.theme.LocalPalette
import dev.kardoso.marginalia.core.ui.Hairline
import dev.kardoso.marginalia.core.ui.IconAction
import dev.kardoso.marginalia.core.ui.SecondaryButton
import dev.kardoso.marginalia.core.ui.SectionLabel
import dev.kardoso.marginalia.library.BookFormat
import dev.kardoso.marginalia.settings.AppearanceControls
import kotlin.math.roundToInt

@Composable
fun ReaderOverlay(viewModel: ReaderViewModel, onClose: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val bookmarked by viewModel.bookmarked.collectAsStateWithLifecycle()
    val motion = LocalMotionMillis.current
    val showBars = state.barsVisible && state.panel == ReaderPanel.NONE

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = showBars,
            enter = fadeIn(tween(motion)) + slideInVertically(tween(motion)) { -it / 2 },
            exit = fadeOut(tween(motion)) + slideOutVertically(tween(motion)) { -it / 2 },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            TopBar(
                state = state,
                bookmarked = bookmarked,
                onBack = onClose,
                onSearch = viewModel::openSearch,
                onAppearance = viewModel::openAppearance,
                onBookmark = viewModel::toggleBookmark,
            )
        }
        AnimatedVisibility(
            visible = showBars,
            enter = fadeIn(tween(motion)) + slideInVertically(tween(motion)) { it / 2 },
            exit = fadeOut(tween(motion)) + slideOutVertically(tween(motion)) { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            BottomBar(
                state = state,
                onSeek = viewModel::seekTo,
                onContents = { viewModel.openContents(ContentsTab.TOC) },
                onNotes = {
                    viewModel.openContents(if (state.format == BookFormat.PDF) ContentsTab.BOOKMARKS else ContentsTab.ANNOTATIONS)
                },
            )
        }

        state.selection?.let { selection ->
            SelectionMenu(
                selection = selection,
                onColor = viewModel::highlight,
                onNote = viewModel::editNote,
                onCopy = viewModel::copySelection,
                onDelete = viewModel::deleteSelectedHighlight,
            )
        }

        Sheet(visible = state.panel == ReaderPanel.APPEARANCE, scrim = false, onDismiss = viewModel::closePanel) {
            AppearanceControls(
                prefs = prefs,
                onChange = viewModel::updatePreferences,
                onReset = viewModel::resetPreferences,
                showTextOptions = state.format == BookFormat.EPUB,
            )
        }

        Sheet(visible = state.footnote != null, scrim = true, onDismiss = viewModel::dismissFootnote) {
            state.footnote?.let { FootnoteContent(it, onClose = viewModel::dismissFootnote, onGoTo = viewModel::goToFootnote) }
        }

        AnimatedVisibility(
            visible = state.panel == ReaderPanel.CONTENTS,
            enter = fadeIn(tween(motion)),
            exit = fadeOut(tween(motion)),
        ) {
            ContentsPanel(viewModel = viewModel, state = state)
        }
        AnimatedVisibility(
            visible = state.panel == ReaderPanel.SEARCH,
            enter = fadeIn(tween(motion)),
            exit = fadeOut(tween(motion)),
        ) {
            SearchPanel(viewModel = viewModel, search = state.search)
        }
    }

    state.noteEditor?.let { editor ->
        NoteEditorDialog(
            excerpt = editor.excerpt,
            initial = editor.initial,
            onSave = viewModel::saveNote,
            onDismiss = viewModel::dismissNoteEditor,
        )
    }
}

@Composable
private fun TopBar(
    state: ReaderUiState,
    bookmarked: Boolean,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onAppearance: () -> Unit,
    onBookmark: () -> Unit,
) {
    val palette = LocalPalette.current
    Column(Modifier.fillMaxWidth().background(palette.background).statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            IconAction(R.drawable.ic_arrow_back, stringResource(R.string.reader_back), onBack)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    state.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                state.chapter?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = palette.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (state.searchable) {
                IconAction(R.drawable.ic_search, stringResource(R.string.reader_search), onSearch)
            }
            val appearanceLabel = stringResource(R.string.reader_appearance)
            TextButton(onClick = onAppearance, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.medium) {
                Text(
                    "Aa",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = Literata, fontSize = 18.sp),
                    color = palette.text,
                    modifier = Modifier.semantics { contentDescription = appearanceLabel },
                )
            }
            IconAction(
                if (bookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark,
                stringResource(if (bookmarked) R.string.reader_bookmark_remove else R.string.reader_bookmark_add),
                onBookmark,
            )
        }
        Hairline()
    }
}

@Composable
private fun BottomBar(
    state: ReaderUiState,
    onSeek: (Float) -> Unit,
    onContents: () -> Unit,
    onNotes: () -> Unit,
) {
    val palette = LocalPalette.current
    var dragging by remember { mutableStateOf<Float?>(null) }
    Column(Modifier.fillMaxWidth().background(palette.background)) {
        Hairline()
        Column(Modifier.navigationBarsPadding().padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 8.dp)) {
            ProgressSlider(
                value = dragging ?: state.progression,
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    dragging?.let(onSeek)
                    dragging = null
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconAction(R.drawable.ic_toc, stringResource(R.string.reader_contents), onContents)
                Text(
                    text = dragging?.let { stringResource(R.string.library_percent, (it * 100).roundToInt()) } ?: positionLabel(state),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = palette.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconAction(
                    if (state.format == BookFormat.PDF) R.drawable.ic_bookmark else R.drawable.ic_edit_note,
                    stringResource(if (state.format == BookFormat.PDF) R.string.contents_bookmarks_plain else R.string.reader_notes),
                    onNotes,
                )
            }
        }
    }
}

/** Barra de progresso do design: trilho de 4 dp e polegar redondo de 16 dp, ambos na cor de marca. */
@Composable
private fun ProgressSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val interaction = remember { MutableInteractionSource() }
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interaction,
        modifier = modifier,
        thumb = {
            Box(Modifier.size(16.dp).background(palette.brand, CircleShape))
        },
        track = { sliderState ->
            val fraction = sliderState.value.coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(palette.line, RoundedCornerShape(2.dp)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(4.dp)
                        .background(palette.brand, RoundedCornerShape(2.dp)),
                )
            }
        },
    )
}

@Composable
private fun positionLabel(state: ReaderUiState): String {
    val position = state.position
    val page = if (position != null && state.totalPositions > 0) {
        stringResource(R.string.reader_page_of, position, state.totalPositions)
    } else {
        stringResource(R.string.library_percent, (state.progression * 100).roundToInt())
    }
    val minutes = state.minutesLeftInChapter ?: return page
    return stringResource(R.string.reader_page_minutes, page, minutes)
}

/** Menu flutuante sobre a seleção: 4 cores, nota, copiar e, num destaque existente, excluir. */
@Composable
private fun SelectionMenu(
    selection: SelectionUi,
    onColor: (HighlightColor) -> Unit,
    onNote: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalPalette.current
    val density = LocalDensity.current
    var menuSize by remember { mutableStateOf(IntSize.Zero) }
    val shape = MaterialTheme.shapes.medium
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val bounds = with(density) { IntSize(maxWidth.roundToPx(), maxHeight.roundToPx()) }
        val gap = with(density) { 12.dp.roundToPx() }
        val margin = with(density) { 12.dp.roundToPx() }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .onSizeChanged { menuSize = it }
                .offset { menuOffset(selection.rect, menuSize, bounds, gap, margin) }
                .shadow(elevation = 3.dp, shape = shape, ambientColor = palette.text, spotColor = palette.text)
                .background(palette.background, shape)
                .border(1.dp, palette.lineStrong, shape)
                .padding(horizontal = 4.dp),
        ) {
            HighlightColor.entries.forEach { color ->
                ColorDot(color = color, selected = color == selection.color) { onColor(color) }
            }
            Spacer(Modifier.width(1.dp).heightIn(min = 28.dp).background(palette.line))
            TextButton(onClick = onNote, modifier = Modifier.heightIn(min = 44.dp)) {
                Text(stringResource(R.string.selection_note), style = MaterialTheme.typography.bodyMedium, color = palette.text)
            }
            if (selection.highlightId != null) {
                IconAction(R.drawable.ic_delete, stringResource(R.string.action_delete), onDelete)
            } else {
                TextButton(onClick = onCopy, modifier = Modifier.heightIn(min = 44.dp)) {
                    Text(stringResource(R.string.selection_copy), style = MaterialTheme.typography.bodyMedium, color = palette.text)
                }
            }
        }
    }
}

private fun menuOffset(rect: RectF?, menu: IntSize, bounds: IntSize, gap: Int, margin: Int): IntOffset {
    if (rect == null) {
        return IntOffset((bounds.width - menu.width) / 2, bounds.height - menu.height - margin * 6)
    }
    val x = (rect.centerX() - menu.width / 2f).roundToInt().coerceIn(margin, (bounds.width - menu.width - margin).coerceAtLeast(margin))
    val above = rect.top.roundToInt() - menu.height - gap
    val y = if (above >= margin * 4) above else (rect.bottom.roundToInt() + gap * 3).coerceAtMost(bounds.height - menu.height - margin)
    return IntOffset(x, y)
}

@Composable
private fun FootnoteContent(footnote: FootnoteUi, onClose: () -> Unit, onGoTo: () -> Unit) {
    val palette = LocalPalette.current
    val text = remember(footnote.content) {
        runCatching { AnnotatedString.fromHtml(footnote.content) }.getOrElse { AnnotatedString(footnote.content) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(stringResource(R.string.footnote_title), Modifier.weight(1f))
            IconAction(R.drawable.ic_close, stringResource(R.string.action_close), onClose)
        }
        Text(
            text = text,
            style = ExcerptStyle.copy(fontSize = 17.sp, lineHeight = 26.sp),
            color = palette.text,
            modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton(stringResource(R.string.footnote_go), onClick = onGoTo, minHeight = 44.dp)
        }
    }
}

/**
 * Folha inferior no tema ativo, cantos de 12 dp e fio no topo. Sem sombra.
 * Com `scrim`, escurece o texto atrás; sem, um toque fora também fecha.
 */
@Composable
private fun Sheet(visible: Boolean, scrim: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val palette = LocalPalette.current
    val motion = LocalMotionMillis.current
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = visible, enter = fadeIn(tween(motion)), exit = fadeOut(tween(motion))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(if (scrim) palette.scrim else Color.Transparent)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            )
        }
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(motion)) { it },
            exit = slideOutVertically(tween(motion)) { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            val shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.background, shape)
                    .border(1.dp, palette.line, shape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 32.dp, height = 4.dp)
                        .background(palette.line, RoundedCornerShape(2.dp)),
                )
                Spacer(Modifier.heightIn(min = 16.dp))
                content()
            }
        }
    }
}
