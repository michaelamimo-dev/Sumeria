package com.michaelamimo.sumeria.ui.reader

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.michaelamimo.sumeria.domain.model.BookFormat
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import org.readium.adapter.pdfium.navigator.PdfiumEngineProvider
import org.readium.r2.navigator.epub.EpubDefaults
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.FontFamily as ReadiumFontFamily
import org.readium.r2.navigator.preferences.TextAlign as ReadiumTextAlign
import org.readium.r2.navigator.preferences.Theme as ReadiumTheme
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.NavigatorFragment
import org.readium.r2.navigator.SelectableNavigator
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

@Composable
fun ReadiumPublicationView(
    publication: Publication,
    format: BookFormat,
    initialLocator: Locator?,
    preferences: ReaderPreferences,
    onLocatorChanged: (Locator) -> Unit,
    onContentTap: () -> Unit,
    isDictionarySelectionActive: () -> Boolean,
    onSelectedWord: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val containerId = remember(publication) { View.generateViewId() }
    val tag = remember(publication) { "sumeria-publication-${System.identityHashCode(publication)}" }

    AndroidView(
        modifier = modifier,
        factory = { viewContext -> FragmentContainerView(viewContext).apply { id = containerId } },
        update = { container ->
            val host = activity ?: return@AndroidView
            val manager = host.supportFragmentManager
            if (manager.isStateSaved) return@AndroidView
            val existing = manager.findFragmentByTag(tag)
            if (existing != null) {
                if (existing is EpubNavigatorFragment && format == BookFormat.EPUB) {
                    existing.submitPreferences(preferences.toReadiumPreferences())
                }
                return@AndroidView
            }
            val factory = when (format) {
                BookFormat.EPUB -> EpubNavigatorFactory(
                    publication,
                    EpubNavigatorFactory.Configuration(defaults = EpubDefaults(scroll = preferences.readingStyle == ReadingStyle.Scroll))
                ).createFragmentFactory(
                    initialLocator = initialLocator,
                    initialPreferences = preferences.toReadiumPreferences()
                )
                BookFormat.PDF -> PdfNavigatorFactory(
                    publication = publication,
                    pdfEngineProvider = PdfiumEngineProvider()
                ).createFragmentFactory(initialLocator = initialLocator)
            }
            manager.fragmentFactory = factory
            val fragmentClass = when (format) {
                BookFormat.EPUB -> EpubNavigatorFragment::class.java
                BookFormat.PDF -> PdfNavigatorFragment::class.java
            }
            manager.beginTransaction()
                .add(container.id, fragmentClass, Bundle(), tag)
                .commitNow()
            val navigatorFragment = manager.findFragmentByTag(tag) as? NavigatorFragment
            navigatorFragment?.let { currentNavigator ->
                if (currentNavigator is VisualNavigator) {
                    currentNavigator.addInputListener(object : InputListener {
                        override fun onTap(event: TapEvent): Boolean {
                            if (isDictionarySelectionActive()) {
                                val selectable = currentNavigator as? SelectableNavigator
                                currentNavigator.viewLifecycleOwner.lifecycleScope.launch {
                                    onSelectedWord(selectable?.currentSelection()?.locator?.text?.highlight)
                                }
                            } else {
                                onContentTap()
                            }
                            return true
                        }
                    })
                }
                currentNavigator.viewLifecycleOwner.lifecycleScope.launch {
                    currentNavigator.viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        currentNavigator.currentLocator.collect { locator -> onLocatorChanged(locator) }
                    }
                }
            }
        }
    )

    DisposableEffect(activity, tag) {
        onDispose {
            val manager = activity?.supportFragmentManager ?: return@onDispose
            if (!manager.isStateSaved) {
                manager.findFragmentByTag(tag)?.let { fragment ->
                    manager.beginTransaction().remove(fragment).commitNow()
                }
            }
        }
    }
}

private fun ReaderPreferences.toReadiumPreferences() = EpubPreferences(
    fontFamily = when (font) {
        ReaderFont.OpenDyslexic -> ReadiumFontFamily.OPEN_DYSLEXIC
        ReaderFont.AccessibleDfa -> ReadiumFontFamily.ACCESSIBLE_DFA
        ReaderFont.WriterDuospace -> ReadiumFontFamily.IA_WRITER_DUOSPACE
        ReaderFont.Fantasy -> ReadiumFontFamily.FANTASY
        ReaderFont.Serif, ReaderFont.Georgia, ReaderFont.BookSerif -> ReadiumFontFamily.SERIF
        ReaderFont.Sans, ReaderFont.Rounded, ReaderFont.Condensed -> ReadiumFontFamily.SANS_SERIF
        ReaderFont.Mono -> ReadiumFontFamily.MONOSPACE
        ReaderFont.Cursive -> ReadiumFontFamily.CURSIVE
        ReaderFont.System -> null
    },
    fontSize = fontSize / 18.0,
    lineHeight = lineSpacing.toDouble(),
    pageMargins = (horizontalMargin / 24.0).coerceIn(0.5, 1.5),
    scroll = readingStyle == ReadingStyle.Scroll,
    textAlign = if (textAlignment == TextAlignment.Justified) {
        ReadiumTextAlign.JUSTIFY
    } else {
        ReadiumTextAlign.START
    },
    theme = when (theme) {
        ReaderTheme.Sepia -> ReadiumTheme.SEPIA
        ReaderTheme.Dark, ReaderTheme.MidnightSumeria -> ReadiumTheme.DARK
        ReaderTheme.Light -> ReadiumTheme.LIGHT
    },
    publisherStyles = false
)

private fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return current as? FragmentActivity
}
