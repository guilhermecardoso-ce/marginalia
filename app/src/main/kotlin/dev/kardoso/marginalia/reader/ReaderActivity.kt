package dev.kardoso.marginalia.reader

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.kardoso.marginalia.R
import dev.kardoso.marginalia.core.readium.toEpubPreferences
import dev.kardoso.marginalia.core.theme.MarginaliaTheme
import dev.kardoso.marginalia.library.BookFormat
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.HyperlinkNavigator
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.SelectableNavigator
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.css.FontStyle
import org.readium.r2.navigator.html.HtmlDecorationTemplates
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.util.DirectionalNavigationAdapter
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.toUri

/**
 * Leitor em tela cheia: o navegador do Readium embaixo e a interface em Compose por cima.
 * Nada fica fixo na tela durante a leitura; um toque no centro mostra e esconde as barras.
 */
@AndroidEntryPoint
class ReaderActivity : AppCompatActivity() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReaderEntryPoint {
        fun readerRepository(): ReaderRepository
    }

    private val viewModel: ReaderViewModel by viewModels()
    private lateinit var navigator: VisualNavigator
    private var actionMode: ActionMode? = null

    /** O fragmento é criado antes da injeção; este objeto só repassa as chamadas ao ViewModel. */
    private val navigatorListener = object : EpubNavigatorFragment.Listener, PdfNavigatorFragment.Listener {
        override fun shouldFollowInternalLink(link: Link, context: HyperlinkNavigator.LinkContext?): Boolean =
            viewModel.shouldFollowInternalLink(link, context)

        override fun onExternalLinkActivated(url: AbsoluteUrl) = viewModel.onExternalLinkActivated(url)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val bookId = intent.getLongExtra(EXTRA_BOOK_ID, -1L)
        val session = EntryPointAccessors.fromApplication(applicationContext, ReaderEntryPoint::class.java)
            .readerRepository()[bookId]

        if (session == null) {
            // Processo recriado sem a publicação aberta: volta para a biblioteca.
            supportFragmentManager.fragmentFactory = EpubNavigatorFragment.createDummyFactory()
            super.onCreate(savedInstanceState)
            finish()
            return
        }

        supportFragmentManager.fragmentFactory = when (session.format) {
            BookFormat.EPUB -> session.epubFactory.createFragmentFactory(
                initialLocator = session.lastLocator,
                initialPreferences = session.preferences.toEpubPreferences(),
                listener = navigatorListener,
                configuration = epubConfiguration(),
            )
            BookFormat.PDF -> session.pdfFactory.createFragmentFactory(
                initialLocator = session.lastLocator,
                listener = navigatorListener,
            )
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reader)

        if (savedInstanceState == null) {
            val fragmentClass = when (session.format) {
                BookFormat.EPUB -> EpubNavigatorFragment::class.java
                BookFormat.PDF -> PdfNavigatorFragment::class.java
            }
            supportFragmentManager.commitNow {
                add(R.id.reader_container, fragmentClass, Bundle(), NAVIGATOR_TAG)
            }
        }
        navigator = supportFragmentManager.findFragmentByTag(NAVIGATOR_TAG) as VisualNavigator

        val container = findViewById<View>(R.id.reader_container)
        ViewCompat.setOnApplyWindowInsetsListener(container) { view, insets ->
            // Recortes de câmera nunca cobrem o texto; as barras do sistema ficam escondidas.
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            view.updatePadding(left = cutout.left, top = cutout.top, right = cutout.right, bottom = cutout.bottom)
            insets
        }

        setupInput()
        setupOverlay()
        observe()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!viewModel.onBack()) finish()
                }
            },
        )
    }

    private fun epubConfiguration() = EpubNavigatorFragment.Configuration {
        servedAssets += "fonts/.*"
        selectionActionModeCallback = selectionCallback
        decorationTemplates = HtmlDecorationTemplates.defaultTemplates(alpha = 1.0, experimentalPositioning = true)
        addFontFamilyDeclaration(FontFamily("Literata")) {
            addFontFace {
                addSource("fonts/Literata.ttf")
                setFontStyle(FontStyle.NORMAL)
                setFontWeight(200..900)
            }
            addFontFace {
                addSource("fonts/Literata-Italic.ttf")
                setFontStyle(FontStyle.ITALIC)
                setFontWeight(200..900)
            }
        }
        addFontFamilyDeclaration(FontFamily("Inter")) {
            addFontFace {
                addSource("fonts/Inter.ttf")
                setFontStyle(FontStyle.NORMAL)
                setFontWeight(100..900)
            }
            addFontFace {
                addSource("fonts/Inter-Italic.ttf")
                setFontStyle(FontStyle.ITALIC)
                setFontWeight(100..900)
            }
        }
    }

    private fun setupInput() {
        (navigator as? OverflowableNavigator)?.let {
            navigator.addInputListener(DirectionalNavigationAdapter(it, animatedTransition = true))
        }
        navigator.addInputListener(
            object : InputListener {
                override fun onTap(event: TapEvent): Boolean {
                    viewModel.onCenterTap()
                    return true
                }
            },
        )
        (navigator as? DecorableNavigator)?.addDecorationListener(HIGHLIGHTS_GROUP, viewModel)
    }

    private fun setupOverlay() {
        findViewById<ComposeView>(R.id.reader_overlay).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val prefs by viewModel.preferences.collectAsStateWithLifecycle()
                MarginaliaTheme(prefs.theme) {
                    ReaderOverlay(viewModel = viewModel, onClose = { finish() })
                }
            }
        }
    }

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    navigator.currentLocator.collect { viewModel.onLocatorChanged(it) }
                }
                launch {
                    viewModel.preferences.collect { prefs ->
                        val palette = prefs.theme.palette
                        window.decorView.setBackgroundColor(palette.background.toArgb())
                        findViewById<View>(R.id.reader_container).setBackgroundColor(palette.background.toArgb())
                        WindowCompat.getInsetsController(window, window.decorView).apply {
                            isAppearanceLightStatusBars = !palette.isDark
                            isAppearanceLightNavigationBars = !palette.isDark
                        }
                        (navigator as? EpubNavigatorFragment)?.submitPreferences(prefs.toEpubPreferences())
                    }
                }
                launch {
                    viewModel.state.map { it.barsVisible }.distinctUntilChanged().collect { visible ->
                        setSystemBarsVisible(visible)
                    }
                }
                launch {
                    (navigator as? DecorableNavigator)?.let { decorable ->
                        viewModel.decorations.collect { decorable.applyDecorations(it, HIGHLIGHTS_GROUP) }
                    }
                }
                launch {
                    viewModel.commands.collect(::handle)
                }
            }
        }
    }

    private suspend fun handle(command: ReaderCommand) {
        when (command) {
            is ReaderCommand.Go -> navigator.go(command.locator, animated = false)
            is ReaderCommand.GoToLink -> navigator.go(command.link, animated = false)
            is ReaderCommand.Copy -> {
                getSystemService<ClipboardManager>()?.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), command.text))
            }
            is ReaderCommand.OpenExternal -> {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, command.url.toUri()))
                } catch (_: ActivityNotFoundException) {
                    // Sem navegador instalado: ignora.
                }
            }
            ReaderCommand.ClearSelection -> {
                actionMode?.finish()
                actionMode = null
                (navigator as? SelectableNavigator)?.clearSelection()
            }
            ReaderCommand.Finish -> finish()
        }
    }

    private fun setSystemBarsVisible(visible: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (visible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    /**
     * A barra flutuante do sistema fica vazia; no lugar dela aparece o menu de seleção do app,
     * nas cores do tema ativo.
     */
    private val selectionCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            actionMode = mode
            menu.clear()
            refreshSelection()
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            menu.clear()
            refreshSelection()
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = false

        override fun onDestroyActionMode(mode: ActionMode) {
            actionMode = null
            viewModel.onSelectionCleared()
        }
    }

    private fun refreshSelection() {
        lifecycleScope.launch {
            viewModel.onSelection((navigator as? SelectableNavigator)?.currentSelection())
        }
    }

    override fun onStop() {
        super.onStop()
        if (::navigator.isInitialized) viewModel.onLocatorChanged(navigator.currentLocator.value)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing && ::navigator.isInitialized) viewModel.close()
    }

    companion object {
        const val EXTRA_BOOK_ID = "bookId"
        private const val NAVIGATOR_TAG = "navigator"
        private const val HIGHLIGHTS_GROUP = "highlights"

        fun intent(context: Context, bookId: Long) =
            Intent(context, ReaderActivity::class.java).putExtra(EXTRA_BOOK_ID, bookId)
    }
}
