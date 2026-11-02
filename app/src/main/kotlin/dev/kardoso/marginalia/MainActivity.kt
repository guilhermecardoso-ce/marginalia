package dev.kardoso.marginalia

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kardoso.marginalia.annotations.AnnotationsHomeScreen
import dev.kardoso.marginalia.annotations.BookNotesScreen
import dev.kardoso.marginalia.annotations.ExportScreen
import dev.kardoso.marginalia.core.prefs.PreferencesRepository
import dev.kardoso.marginalia.core.theme.LocalMotionMillis
import dev.kardoso.marginalia.core.theme.LocalPalette
import dev.kardoso.marginalia.core.theme.MarginaliaTheme
import dev.kardoso.marginalia.core.theme.ReaderTheme
import dev.kardoso.marginalia.core.ui.Hairline
import dev.kardoso.marginalia.library.LibraryScreen
import dev.kardoso.marginalia.reader.ReaderActivity
import dev.kardoso.marginalia.settings.SettingsScreen
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable data object LibraryRoute
@Serializable data object AnnotationsRoute
@Serializable data object SettingsRoute
@Serializable data class BookNotesRoute(val bookId: Long)
@Serializable data class ExportRoute(val bookId: Long)

@HiltViewModel
class MainViewModel @Inject constructor(preferences: PreferencesRepository) : ViewModel() {
    val theme: StateFlow<ReaderTheme?> = preferences.reading
        .map<_, ReaderTheme?> { it.theme }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val theme by viewModel.theme.collectAsStateWithLifecycle()
            MarginaliaTheme(theme ?: ReaderTheme.Default) {
                val palette = LocalPalette.current
                SideEffect {
                    window.decorView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !palette.isDark
                        isAppearanceLightNavigationBars = !palette.isDark
                    }
                }
                if (theme != null) AppRoot()
            }
        }
    }
}

private enum class Tab(val route: Any, @StringRes val label: Int, @DrawableRes val icon: Int) {
    LIBRARY(LibraryRoute, R.string.tab_library, R.drawable.ic_local_library),
    ANNOTATIONS(AnnotationsRoute, R.string.tab_annotations, R.drawable.ic_edit_note),
    SETTINGS(SettingsRoute, R.string.tab_settings, R.drawable.ic_tune),
}

@Composable
private fun AppRoot() {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val palette = LocalPalette.current
    val motion = LocalMotionMillis.current
    val showMessage: (String) -> Unit = { message ->
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }
    val openReader: (Long) -> Unit = { bookId -> context.startActivity(ReaderActivity.intent(context, bookId)) }

    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val currentTab = Tab.entries.firstOrNull { tab -> destination?.hasRoute(tab.route::class) == true }

    Scaffold(
        containerColor = palette.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = MaterialTheme.shapes.medium,
                    containerColor = palette.text,
                    contentColor = palette.background,
                )
            }
        },
        bottomBar = {
            if (currentTab != null) {
                BottomTabs(current = currentTab) { tab ->
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = LibraryRoute,
            enterTransition = { if (motion == 0) EnterTransition.None else fadeIn(tween(motion)) },
            exitTransition = { if (motion == 0) ExitTransition.None else fadeOut(tween(motion)) },
            modifier = Modifier.fillMaxSize().background(palette.background),
        ) {
            composable<LibraryRoute> {
                LibraryScreen(contentPadding = padding.withStatusBar(), onOpenReader = openReader, showMessage = showMessage)
            }
            composable<AnnotationsRoute> {
                AnnotationsHomeScreen(
                    contentPadding = padding.withStatusBar(),
                    onOpenBook = { navController.navigate(BookNotesRoute(it)) },
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(contentPadding = padding.withStatusBar())
            }
            composable<BookNotesRoute> { entry ->
                val route = entry.toRoute<BookNotesRoute>()
                BookNotesScreen(
                    onBack = { navController.popBackStack() },
                    onExport = { navController.navigate(ExportRoute(route.bookId)) },
                    onOpenReader = openReader,
                    showMessage = showMessage,
                )
            }
            composable<ExportRoute> {
                ExportScreen(onClose = { navController.popBackStack() }, showMessage = showMessage)
            }
        }
    }
}

@Composable
private fun PaddingValues.withStatusBar(): PaddingValues = PaddingValues(
    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
    bottom = calculateBottomPadding(),
)

@Composable
private fun BottomTabs(current: Tab, onSelect: (Tab) -> Unit) {
    val palette = LocalPalette.current
    Column(Modifier.background(palette.background)) {
        Hairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(72.dp)
                .padding(horizontal = 12.dp),
        ) {
            Tab.entries.forEach { tab ->
                val selected = tab == current
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .selectable(selected = selected, role = Role.Tab) { onSelect(tab) },
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(width = 56.dp, height = 32.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(if (selected) palette.selected else Color.Transparent),
                    ) {
                        Icon(
                            painterResource(tab.icon),
                            contentDescription = null,
                            tint = if (selected) palette.text else palette.secondary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        stringResource(tab.label),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        color = if (selected) palette.text else palette.secondary,
                    )
                }
            }
        }
    }
}
