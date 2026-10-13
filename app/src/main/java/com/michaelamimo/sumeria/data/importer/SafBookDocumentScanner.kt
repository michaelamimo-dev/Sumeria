package com.michaelamimo.sumeria.data.importer

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.michaelamimo.sumeria.domain.repository.BookDocumentScanner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SafBookDocumentScanner @Inject constructor(
    @ApplicationContext private val context: Context
) : BookDocumentScanner {
    override suspend fun scan(treeUri: String): List<String> = withContext(Dispatchers.IO) {
        val rootUri = Uri.parse(treeUri)
        val rootDocumentId = DocumentsContract.getTreeDocumentId(rootUri)
        val pendingIds = ArrayDeque<String>().apply { add(rootDocumentId) }
        val visitedIds = mutableSetOf<String>()
        val books = mutableListOf<Pair<String, String>>()
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        while (pendingIds.isNotEmpty()) {
            val parentId = pendingIds.removeFirst()
            if (!visitedIds.add(parentId)) continue
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, parentId)
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(idColumn) ?: continue
                    val name = cursor.getString(nameColumn).orEmpty()
                    val mimeType = cursor.getString(mimeColumn).orEmpty()
                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        pendingIds.add(documentId)
                    } else if (isSupportedBook(name, mimeType)) {
                        val documentUri = DocumentsContract.buildDocumentUriUsingTree(rootUri, documentId)
                        books += name to documentUri.toString()
                    }
                }
            }
        }
        books.distinctBy { it.second }.sortedBy { it.first.lowercase() }.map { it.second }
    }

    private fun isSupportedBook(name: String, mimeType: String): Boolean {
        val extension = name.substringAfterLast('.', "").lowercase()
        return extension == "epub" || extension == "pdf" ||
            mimeType.equals("application/pdf", ignoreCase = true) ||
            mimeType.equals("application/epub+zip", ignoreCase = true) ||
            mimeType.equals("application/epub", ignoreCase = true)
    }
}
