package dev.kardoso.marginalia.annotations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.kardoso.marginalia.R
import dev.kardoso.marginalia.core.theme.ExcerptStyle
import dev.kardoso.marginalia.core.theme.LocalPalette
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Diálogo para escrever ou editar a nota de um destaque. */
@Composable
fun NoteEditorDialog(
    excerpt: String,
    initial: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    var text by rememberSaveable { mutableStateOf(initial.orEmpty()) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.background,
        titleContentColor = palette.text,
        textContentColor = palette.text,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
        title = { Text(stringResource(R.string.note_title), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    excerpt,
                    style = ExcerptStyle.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
                    color = palette.secondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.note_hint), color = palette.secondary) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.text,
                        unfocusedBorderColor = palette.lineStrong,
                        cursorColor = palette.brand,
                        focusedTextColor = palette.text,
                        unfocusedTextColor = palette.text,
                    ),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).focusRequester(focus),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) {
                Text(stringResource(R.string.action_save), color = palette.text, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = palette.secondary, style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

/** "p. 47 · hoje" */
@Composable
fun annotationMeta(position: Int?, createdAt: Long): String {
    val date = relativeDate(createdAt)
    return if (position != null) stringResource(R.string.meta_page_date, position, date) else date
}

@Composable
private fun relativeDate(millis: Long): String {
    val zone = ZoneId.systemDefault()
    val day = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    return when (day) {
        today -> stringResource(R.string.date_today)
        today.minusDays(1) -> stringResource(R.string.date_yesterday)
        else -> {
            val pattern = if (day.year == today.year) "d MMM" else "d MMM yyyy"
            day.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
        }
    }
}
