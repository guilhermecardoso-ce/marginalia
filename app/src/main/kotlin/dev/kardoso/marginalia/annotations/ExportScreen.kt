package dev.kardoso.marginalia.annotations

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kardoso.marginalia.R
import dev.kardoso.marginalia.core.theme.CodeStyle
import dev.kardoso.marginalia.core.theme.LocalPalette
import dev.kardoso.marginalia.core.ui.Hairline
import dev.kardoso.marginalia.core.ui.IconAction
import dev.kardoso.marginalia.core.ui.PrimaryButton
import dev.kardoso.marginalia.core.ui.SecondaryButton
import dev.kardoso.marginalia.core.ui.SectionLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ExportScreen(
    onClose: () -> Unit,
    showMessage: (String) -> Unit,
    viewModel: BookNotesViewModel = hiltViewModel(),
) {
    val state by viewModel.export.collectAsStateWithLifecycle()
    val palette = LocalPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedText = stringResource(R.string.export_saved, state.exportedCount)
    val failedText = stringResource(R.string.export_failed)

    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val markdown = state.markdown
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(markdown.toByteArray()) }
                }.isSuccess
            }
            showMessage(if (ok) savedText else failedText)
        }
    }
    val shareTitle = stringResource(R.string.export_share)

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            IconAction(R.drawable.ic_close, stringResource(R.string.action_close), onClose)
            Text(stringResource(R.string.export_title), style = MaterialTheme.typography.titleLarge, color = palette.text)
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Column {
                SectionLabel(stringResource(R.string.export_include), Modifier.padding(bottom = 4.dp))
                OptionRow(stringResource(R.string.export_highlights, state.highlightCount), state.options.highlights) {
                    viewModel.setExportOptions(state.options.copy(highlights = it))
                }
                Hairline()
                OptionRow(stringResource(R.string.export_notes, state.noteCount), state.options.notes) {
                    viewModel.setExportOptions(state.options.copy(notes = it))
                }
                Hairline()
                OptionRow(stringResource(R.string.export_page_numbers), state.options.pageNumbers) {
                    viewModel.setExportOptions(state.options.copy(pageNumbers = it))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(stringResource(R.string.export_preview), Modifier.weight(1f))
                    Text(state.fileName, style = CodeStyle, color = palette.secondary)
                }
                Text(
                    state.markdown,
                    style = CodeStyle,
                    color = palette.text,
                    softWrap = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.code, MaterialTheme.shapes.medium)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }
        }

        Hairline()
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
        ) {
            SecondaryButton(
                text = stringResource(R.string.export_share_action),
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/markdown"
                        putExtra(Intent.EXTRA_TEXT, state.markdown)
                        putExtra(Intent.EXTRA_SUBJECT, state.fileName.removeSuffix(".md"))
                    }
                    context.startActivity(Intent.createChooser(send, shareTitle))
                },
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = stringResource(R.string.export_save_action),
                onClick = { saver.launch(state.fileName) },
                enabled = state.exportedCount > 0,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun OptionRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val palette = LocalPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = palette.text, modifier = Modifier.weight(1f))
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = palette.brand,
                checkmarkColor = palette.onBrand,
                uncheckedColor = palette.lineStrong,
            ),
        )
    }
}
