package com.michaelamimo.sumeria.data.importer

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import com.michaelamimo.sumeria.domain.model.BookFormat
import com.michaelamimo.sumeria.domain.model.LibraryBook
import com.michaelamimo.sumeria.domain.repository.PublicationImporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.adapter.pdfium.document.PdfiumDocumentFactory
import org.readium.r2.shared.publication.services.coverFitting
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.format.Specification
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toAbsoluteUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadiumPublicationImporter @Inject constructor(
    @param:ApplicationContext private val context: Context
) : PublicationImporter {
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

    override suspend fun import(sourceUri: String): LibraryBook = withContext(Dispatchers.IO) {
        val uri = Uri.parse(sourceUri)
        val url = uri.toAbsoluteUrl() ?: throw IllegalArgumentException("The selected file URI is not supported.")
        val asset = assetRetriever.retrieve(url).getOrElse { error ->
            throw IOException("The selected file could not be opened: $error")
        }

        try {
            val format = when {
                asset.format.conformsTo(Specification.Epub) -> BookFormat.EPUB
                asset.format.conformsTo(Specification.Pdf) -> BookFormat.PDF
                else -> throw IllegalArgumentException("Choose an EPUB or PDF file.")
            }
            val publication = publicationOpener.open(asset, allowUserInteraction = false).getOrElse { error ->
                throw IOException("This publication could not be parsed: $error")
            }

            try {
                val metadata = publication.metadata
                val fallbackTitle = queryDisplayName(uri)?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
                    ?: "Untitled publication"
                val title = metadata.title?.takeIf { it.isNotBlank() } ?: fallbackTitle
                val id = contentId(uri)
                val coverUri = publication.coverFitting(android.util.Size(640, 960))
                    ?.let { saveCover(it, id) }

                LibraryBook(
                    id = id,
                    title = title,
                    authors = metadata.authors.map { it.name }.filter { it.isNotBlank() },
                    description = metadata.description,
                    format = format,
                    sourceUri = sourceUri,
                    coverUri = coverUri,
                    genre = metadata.subjects.firstOrNull()?.name,
                    totalPages = metadata.numberOfPages,
                    addedAtMillis = System.currentTimeMillis(),
                    fileSizeBytes = queryFileSize(uri)
                )
            } finally {
                publication.close()
            }
        } finally {
            asset.close()
        }
    }

    private fun queryDisplayName(uri: Uri): String? = context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
    }

    private fun queryFileSize(uri: Uri): Long? = context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.SIZE),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE)).takeIf { it > 0L } else null
    }

    private fun contentId(uri: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")
        context.contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        } ?: throw IOException("The selected publication could not be read.")
        return digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private fun saveCover(bitmap: Bitmap, bookId: String): String {
        val coverDirectory = File(context.filesDir, "covers").apply { mkdirs() }
        val coverFile = File(coverDirectory, "$bookId.jpg")
        FileOutputStream(coverFile).use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)) {
                throw IOException("The publication cover could not be saved.")
            }
        }
        return Uri.fromFile(coverFile).toString()
    }
}
