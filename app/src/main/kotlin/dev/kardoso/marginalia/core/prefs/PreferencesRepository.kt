package dev.kardoso.marginalia.core.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.kardoso.marginalia.core.theme.ReaderTheme
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class PreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val reading: Flow<ReadingPreferences> = dataStore.data.map { it.toReadingPreferences() }

    suspend fun update(transform: (ReadingPreferences) -> ReadingPreferences) {
        dataStore.edit { stored ->
            val next = transform(stored.toReadingPreferences())
            stored[THEME] = next.theme.name
            stored[FONT_SIZE] = next.fontSizeSp
            stored[FONT] = next.font.name
            stored[LINE_SPACING] = next.lineSpacing.name
        }
    }

    suspend fun reset() = update { ReadingPreferences() }

    private fun Preferences.toReadingPreferences(): ReadingPreferences {
        val defaults = ReadingPreferences()
        return ReadingPreferences(
            theme = enumOr(this[THEME], defaults.theme),
            fontSizeSp = this[FONT_SIZE] ?: defaults.fontSizeSp,
            font = enumOr(this[FONT], defaults.font),
            lineSpacing = enumOr(this[LINE_SPACING], defaults.lineSpacing),
        )
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: fallback

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val FONT_SIZE = intPreferencesKey("font_size_sp")
        val FONT = stringPreferencesKey("font")
        val LINE_SPACING = stringPreferencesKey("line_spacing")
    }
}
