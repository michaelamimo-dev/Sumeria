package com.michaelamimo.sumeria.data.reader

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.michaelamimo.sumeria.domain.model.BookFormat
import com.michaelamimo.sumeria.domain.model.LibraryBook
import com.michaelamimo.sumeria.domain.model.ReaderDocument
import com.michaelamimo.sumeria.domain.model.ReaderSection
import com.michaelamimo.sumeria.domain.repository.ReaderContentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.readium.adapter.pdfium.document.PdfiumDocumentFactory
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.format.Specification
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toAbsoluteUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.ByteArrayInputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PublicationReaderContentRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ReaderContentRepository {
    private val httpClient = DefaultHttpClient()
    private val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
    private val publicationOpener = PublicationOpener(
        DefaultPublicationParser(
            context = context,
            httpClient = httpClient,
            assetRetriever = assetRetriever,
            pdfFactory = PdfiumDocumentFactory(context)
        )
    )

    override suspend fun load(book: LibraryBook): ReaderDocument = withContext(Dispatchers.IO) {
        val sourceUri = book.sourceUri ?: throw IOException("The original book file is no longer available.")
        when (book.format) {
            BookFormat.EPUB -> loadEpub(sourceUri)
            BookFormat.PDF -> loadPdf(sourceUri)
        }
    }

    private suspend fun loadEpub(sourceUri: String): ReaderDocument {
        val uri = Uri.parse(sourceUri)
        val url = uri.toAbsoluteUrl() ?: throw IOException("The selected book file cannot be opened.")
        val asset = assetRetriever.retrieve(url).getOrElse { error ->
            throw IOException("The EPUB could not be opened: $error")
        }

        return try {
            if (!asset.format.conformsTo(Specification.Epub)) {
                throw IOException("This file is no longer recognized as an EPUB.")
            }
            val publication = publicationOpener.open(asset, allowUserInteraction = false).getOrElse { error ->
                throw IOException("The EPUB could not be parsed: $error")
            }
            try {
                val coroutineContext = currentCoroutineContext()
                val sections = publication.readingOrder.mapIndexedNotNull { index, link ->
                    coroutineContext.ensureActive()
                    val resource = publication.get(link) ?: return@mapIndexedNotNull null
                    val bytes = try {
                        resource.read().getOrElse { error ->
                            throw IOException("A section of the EPUB could not be read: $error")
                        }
                    } finally {
                        resource.close()
                    }

                    val document = Jsoup.parse(ByteArrayInputStream(bytes), null, "")
                    document.select("script, style, nav, svg, noscript").remove()
                    val heading = document.selectFirst("h1, h2, h3")?.text()?.trim()
                    val extractedParagraphs = document.select("h1, h2, h3, h4, p, blockquote, li, pre")
                        .map { it.text().trim() }
                        .filter { it.isNotBlank() }
                        .distinctConsecutive()
                    val paragraphs = extractedParagraphs.ifEmpty {
                        document.body()?.text()?.trim()?.takeIf { it.isNotBlank() }?.let(::listOf).orEmpty()
                    }
                    if (paragraphs.isEmpty()) return@mapIndexedNotNull null

                    ReaderSection(
                        title = heading?.takeIf { it.isNotBlank() }
                            ?: link.title?.takeIf { it.isNotBlank() }
                            ?: "Section ${index + 1}",
                        paragraphs = paragraphs
                    )
                }
                if (sections.isEmpty()) throw IOException("No readable text was found in this EPUB.")
                ReaderDocument(sections = sections)
            } finally {
                publication.close()
            }
        } finally {
            asset.close()
        }
    }

    private suspend fun loadPdf(sourceUri: String): ReaderDocument {
        initializePdfBox()
        val uri = Uri.parse(sourceUri)
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("The original PDF file is no longer available.")

        return input.use { stream ->
            PDDocument.load(stream).use { document ->
                val stripper = PDFTextStripper()
                val coroutineContext = currentCoroutineContext()
                var hasSelectableText = false
                val sections = buildList {
                    for (pageNumber in 1..document.numberOfPages) {
                        coroutineContext.ensureActive()
                        stripper.startPage = pageNumber
                        stripper.endPage = pageNumber
                        val text = stripper.getText(document).trim()
                        val paragraphs = text.split(Regex("\\n\\s*\\n"))
                            .map(String::trim)
                            .filter(String::isNotBlank)
                        if (paragraphs.isNotEmpty()) hasSelectableText = true
                        add(
                            ReaderSection(
                                title = "Page $pageNumber",
                                paragraphs = paragraphs
                            )
                        )
                    }
                }
                if (!hasSelectableText) throw IOException("No selectable text was found in this PDF.")
                ReaderDocument(sections = sections, totalPages = document.numberOfPages)
            }
        }
    }

    private fun initializePdfBox() {
        if (pdfBoxInitialized) return
        synchronized(PDF_BOX_LOCK) {
            if (!pdfBoxInitialized) {
                PDFBoxResourceLoader.init(context)
                pdfBoxInitialized = true
            }
        }
    }

    private fun List<String>.distinctConsecutive(): List<String> = buildList {
        for (value in this@distinctConsecutive) {
            if (lastOrNull() != value) add(value)
        }
    }

    private companion object {
        val PDF_BOX_LOCK = Any()
        @Volatile var pdfBoxInitialized = false
    }
}
