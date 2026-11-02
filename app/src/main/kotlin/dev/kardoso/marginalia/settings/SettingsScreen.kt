package dev.kardoso.marginalia.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kardoso.marginalia.BuildConfig
import dev.kardoso.marginalia.R
import dev.kardoso.marginalia.core.prefs.PreferencesRepository
import dev.kardoso.marginalia.core.prefs.ReadingPreferences
import dev.kardoso.marginalia.core.theme.LocalPalette
import dev.kardoso.marginalia.core.ui.Hairline
import dev.kardoso.marginalia.core.ui.SectionLabel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
) : ViewModel() {

    val reading: StateFlow<ReadingPreferences> = preferences.reading
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingPreferences())

    fun update(next: ReadingPreferences) {
        viewModelScope.launch { preferences.update { next } }
    }

    fun reset() {
        viewModelScope.launch { preferences.reset() }
    }
}

@Composable
fun SettingsScreen(contentPadding: PaddingValues, viewModel: SettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.reading.collectAsStateWithLifecycle()
    val palette = LocalPalette.current
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            color = palette.text,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(stringResource(R.string.settings_reading_hint), style = MaterialTheme.typography.bodyMedium, color = palette.secondary)
        AppearanceControls(prefs = prefs, onChange = viewModel::update, onReset = viewModel::reset)

        Hairline()
        SectionLabel(stringResource(R.string.settings_about))
        Text(
            stringResource(R.string.settings_about_body, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.text,
        )
        Text(stringResource(R.string.settings_licenses), style = MaterialTheme.typography.bodySmall, color = palette.secondary)
    }
}
