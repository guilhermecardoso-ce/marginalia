package dev.kardoso.marginalia.annotations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kardoso.marginalia.R
import dev.kardoso.marginalia.core.db.BookAnnotationCount
import dev.kardoso.marginalia.core.theme.LocalPalette
import dev.kardoso.marginalia.core.theme.MetaStyle
import dev.kardoso.marginalia.core.ui.BookCover
import dev.kardoso.marginalia.core.ui.EmptyState
import dev.kardoso.marginalia.core.ui.Hairline
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class AnnotationsHomeViewModel @Inject constructor(repository: AnnotationRepository) : ViewModel() {
    val books: StateFlow<List<BookAnnotationCount>?> = repository.annotatedBooks()
        .map<List<BookAnnotationCount>, List<BookAnnotationCount>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** Aba "Anotações": os livros que têm destaques ou notas. */
@Composable
fun AnnotationsHomeScreen(
    contentPadding: PaddingValues,
    onOpenBook: (Long) -> Unit,
    viewModel: AnnotationsHomeViewModel = hiltViewModel(),
) {
    val books by viewModel.books.collectAsStateWithLifecycle()
    val palette = LocalPalette.current
    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        Text(
            stringResource(R.string.annotations_title),
            style = MaterialTheme.typography.headlineMedium,
            color = palette.text,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 8.dp),
        )
        val list = books ?: return@Column
        if (list.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.annotations_empty_title),
                body = stringResource(R.string.annotations_empty_body),
                modifier = Modifier.fillMaxWidth(),
            )
            return@Column
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(list, key = { it.bookId }) { book ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenBook(book.bookId) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    BookCover(book.title, book.coverPath, width = 48.dp, height = 72.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(book.title, style = MaterialTheme.typography.titleMedium, color = palette.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        book.author?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = palette.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(
                            pluralStringResource(R.plurals.annotations_count, book.highlights, book.highlights),
                            style = MetaStyle,
                            color = palette.secondary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                Hairline(Modifier.padding(horizontal = 20.dp))
            }
        }
    }
}
