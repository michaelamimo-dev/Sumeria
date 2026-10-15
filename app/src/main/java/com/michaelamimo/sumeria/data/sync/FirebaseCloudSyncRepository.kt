package com.michaelamimo.sumeria.data.sync

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.michaelamimo.sumeria.data.local.dao.BookDao
import com.michaelamimo.sumeria.data.local.dao.ReaderBookmarkDao
import com.michaelamimo.sumeria.data.local.dao.ReadingProgressDao
import com.michaelamimo.sumeria.data.local.dao.ReadingSessionDao
import com.michaelamimo.sumeria.data.local.dao.UserPreferencesDao
import com.michaelamimo.sumeria.data.local.entity.AuthorsJson
import com.michaelamimo.sumeria.data.local.entity.BookEntity
import com.michaelamimo.sumeria.data.local.entity.ReaderBookmarkEntity
import com.michaelamimo.sumeria.data.local.entity.ReadingProgressEntity
import com.michaelamimo.sumeria.data.local.entity.ReadingSessionEntity
import com.michaelamimo.sumeria.data.local.entity.UserPreferencesEntity
import com.michaelamimo.sumeria.data.local.entity.toEntity
import com.michaelamimo.sumeria.domain.model.BookFormat
import com.michaelamimo.sumeria.domain.model.SessionMode
import com.michaelamimo.sumeria.domain.model.UserPreferences
import com.michaelamimo.sumeria.domain.repository.CloudSyncRepository
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

@Singleton
class FirebaseCloudSyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val bookDao: BookDao,
    private val progressDao: ReadingProgressDao,
    private val sessionDao: ReadingSessionDao,
    private val bookmarkDao: ReaderBookmarkDao,
    private val preferencesDao: UserPreferencesDao
) : CloudSyncRepository {
    private val remoteBookIdAliases = mutableMapOf<String, String>()
    private val uploadedHashes = ConcurrentHashMap<String, Int>()
    private val syncMutex = Mutex()

    override suspend fun syncUser(userId: String) = syncMutex.withLock {
        remoteBookIdAliases.clear()
        if (preferencesDao.get(userId) == null) seedLocalProfile(userId)
        val userRef = users.document(userId)
        val userMetadata = userRef.get().await()
        pullBooks(userId)
        pullProgress(userId)
        pullSessions(userId)
        pullBookmarks(userId)
        pullPreferences(userId)
        uploadLocalSnapshot(userId)
        removeMigratedRemoteBookIds(userId)
        userRef.set(
            mapOf("syncInitialized" to true, "lastSyncAtMillis" to System.currentTimeMillis()),
            SetOptions.merge()
        ).await()
        Unit
    }

    override suspend fun uploadLocalSnapshot(userId: String) {
        var batch = firestore.batch()
        var writes = 0
        val pendingHashes = mutableMapOf<String, Int>()
        fun enqueue(reference: DocumentReference, data: Map<String, Any?>) {
            val hash = data.hashCode()
            if (uploadedHashes[reference.path] == hash) return
            batch.set(reference, data)
            pendingHashes[reference.path] = hash
            writes++
        }
        suspend fun commitIfFull() {
            if (writes >= MAX_BATCH_WRITES) {
                batch.commit().await()
                uploadedHashes.putAll(pendingHashes)
                pendingHashes.clear()
                batch = firestore.batch()
                writes = 0
            }
        }
        bookDao.getAll(userId).forEach { row ->
            enqueue(collection(userId, BOOKS).document(row.id), row.toCloudMap()); commitIfFull()
        }
        progressDao.getAll(userId).forEach { row ->
            enqueue(collection(userId, PROGRESS).document(row.bookId), row.toCloudMap()); commitIfFull()
        }
        sessionDao.getAll(userId).forEach { row ->
            enqueue(collection(userId, SESSIONS).document(row.id), row.toCloudMap()); commitIfFull()
        }
        bookmarkDao.getAll(userId).forEach { row ->
            enqueue(collection(userId, BOOKMARKS).document(row.id), row.toCloudMap()); commitIfFull()
        }
        preferencesDao.get(userId)?.let { row ->
            enqueue(collection(userId, PREFERENCES).document(CURRENT), row.toCloudMap())
        }
        if (writes > 0) {
            batch.commit().await()
            uploadedHashes.putAll(pendingHashes)
        }
    }

    override suspend fun deleteBook(userId: String, bookId: String) {
        listOf(PROGRESS, SESSIONS, BOOKMARKS).forEach { path ->
            val docs = collection(userId, path).whereEqualTo("bookId", bookId).get().await().documents
            docs.chunked(MAX_BATCH_WRITES).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
        }
        uploadedHashes.remove(collection(userId, BOOKS).document(bookId).path)
        uploadedHashes.remove(collection(userId, PROGRESS).document(bookId).path)
        collection(userId, BOOKS).document(bookId).delete().await()
    }

    override suspend fun deleteBookmark(userId: String, bookmarkId: String) {
        uploadedHashes.remove(collection(userId, BOOKMARKS).document(bookmarkId).path)
        collection(userId, BOOKMARKS).document(bookmarkId).delete().await()
    }

    private suspend fun seedLocalProfile(userId: String) {
        if (userId == UserPreferences.LOCAL_PROFILE_ID) return
        bookDao.getAll(UserPreferences.LOCAL_PROFILE_ID).forEach { local ->
            if (bookDao.getById(userId, local.id) == null) bookDao.upsert(local.copy(ownerUserId = userId))
        }
        progressDao.getAll(UserPreferences.LOCAL_PROFILE_ID).forEach { local ->
            if (bookDao.getById(userId, local.bookId) != null && progressDao.get(userId, local.bookId) == null) {
                progressDao.upsert(local.copy(ownerUserId = userId))
            }
        }
        sessionDao.getAll(UserPreferences.LOCAL_PROFILE_ID).forEach { local ->
            if (bookDao.getById(userId, local.bookId) != null) sessionDao.upsert(local.copy(ownerUserId = userId))
        }
        bookmarkDao.getAll(UserPreferences.LOCAL_PROFILE_ID).forEach { local ->
            if (bookDao.getById(userId, local.bookId) != null) bookmarkDao.upsert(local.copy(ownerUserId = userId))
        }
        if (preferencesDao.get(userId) == null) {
            val localPreferences = preferencesDao.get(UserPreferences.LOCAL_PROFILE_ID)
            if (localPreferences != null) preferencesDao.upsert(localPreferences.copy(profileId = userId))
            else preferencesDao.upsert(UserPreferences(profileId = userId).toEntity())
        }
    }

    private suspend fun pullBooks(userId: String) {
        collection(userId, BOOKS).get().await().documents.forEach { doc ->
            val format = doc.getString("format")?.let { runCatching { BookFormat.valueOf(it) }.getOrNull() } ?: return@forEach
            val authors = (doc.get("authors") as? List<*>)?.filterIsInstance<String>().orEmpty()
            val title = doc.getString("title").orEmpty()
            val byCloudId = bookDao.getById(userId, doc.id)
            val matching = byCloudId ?: bookDao.findByMetadata(userId, title, AuthorsJson.encode(authors), format.name)
            val localId = matching?.id ?: doc.id
            if (localId != doc.id) remoteBookIdAliases[doc.id] = localId
            val existing = matching
            val remoteLastOpened = doc.long("lastOpenedAtMillis")
            if (existing != null && (existing.lastOpenedAtMillis ?: existing.addedAtMillis) >
                (remoteLastOpened ?: existing.addedAtMillis)) return@forEach
            bookDao.upsert(BookEntity(
                ownerUserId = userId,
                id = localId,
                title = title,
                authorsJson = AuthorsJson.encode(authors),
                description = doc.getString("description"),
                format = format,
                sourceUri = existing?.sourceUri,
                coverUri = existing?.coverUri,
                genre = doc.getString("genre"),
                totalPages = doc.long("totalPages")?.toInt(),
                addedAtMillis = doc.long("addedAtMillis") ?: System.currentTimeMillis(),
                lastOpenedAtMillis = remoteLastOpened ?: existing?.lastOpenedAtMillis,
                fileSizeBytes = doc.long("fileSizeBytes") ?: existing?.fileSizeBytes ?: 0L
            ))
        }
    }

    private suspend fun pullProgress(userId: String) {
        collection(userId, PROGRESS).get().await().documents.forEach { doc ->
            val cloudBookId = doc.getString("bookId") ?: doc.id
            val bookId = remoteBookIdAliases[cloudBookId] ?: cloudBookId
            if (bookDao.getById(userId, bookId) == null) return@forEach
            val updatedAt = doc.long("updatedAtMillis") ?: 0L
            val local = progressDao.get(userId, bookId)
            if (local != null && local.updatedAtMillis >= updatedAt) return@forEach
            progressDao.upsert(ReadingProgressEntity(
                ownerUserId = userId,
                bookId = bookId,
                locationJson = doc.getString("locationJson"),
                currentPage = doc.long("currentPage")?.toInt(),
                totalPages = doc.long("totalPages")?.toInt(),
                progression = doc.getDouble("progression")?.toFloat() ?: 0f,
                updatedAtMillis = updatedAt
            ))
        }
    }

    private suspend fun pullSessions(userId: String) {
        val localSessions = sessionDao.getAll(userId).associateBy { it.id }
        collection(userId, SESSIONS).get().await().documents.forEach { doc ->
            val cloudBookId = doc.getString("bookId") ?: return@forEach
            val bookId = remoteBookIdAliases[cloudBookId] ?: cloudBookId
            if (bookDao.getById(userId, bookId) == null) return@forEach
            val local = localSessions[doc.id]
            val duration = doc.long("durationMillis") ?: 0L
            if (local != null && local.durationMillis >= duration) return@forEach
            val mode = doc.getString("mode")?.let { runCatching { SessionMode.valueOf(it) }.getOrNull() } ?: SessionMode.TEXT
            sessionDao.upsert(ReadingSessionEntity(
                ownerUserId = userId,
                id = doc.id,
                bookId = bookId,
                startedAtMillis = doc.long("startedAtMillis") ?: return@forEach,
                endedAtMillis = doc.long("endedAtMillis"),
                durationMillis = duration,
                pagesRead = doc.long("pagesRead")?.toInt() ?: 0,
                wordsRead = doc.long("wordsRead")?.toInt() ?: 0,
                mode = mode
            ))
        }
    }

    private suspend fun pullBookmarks(userId: String) {
        val localBookmarks = bookmarkDao.getAll(userId).associateBy { it.id }
        collection(userId, BOOKMARKS).get().await().documents.forEach { doc ->
            val cloudBookId = doc.getString("bookId") ?: return@forEach
            val bookId = remoteBookIdAliases[cloudBookId] ?: cloudBookId
            if (bookDao.getById(userId, bookId) == null || doc.id in localBookmarks) return@forEach
            bookmarkDao.upsert(ReaderBookmarkEntity(
                ownerUserId = userId,
                id = doc.id,
                bookId = bookId,
                locationJson = doc.getString("locationJson").orEmpty(),
                chapterTitle = doc.getString("chapterTitle"),
                excerpt = doc.getString("excerpt"),
                createdAtMillis = doc.long("createdAtMillis") ?: 0L
            ))
        }
    }

    private suspend fun pullPreferences(userId: String) {
        val doc = collection(userId, PREFERENCES).document(CURRENT).get().await()
        if (!doc.exists()) return
        val updatedAt = doc.long("updatedAtMillis") ?: 0L
        val local = preferencesDao.get(userId)
        if (local != null && local.updatedAtMillis >= updatedAt) return
        preferencesDao.upsert(UserPreferencesEntity(
            profileId = userId,
            dailyGoalMinutes = doc.long("dailyGoalMinutes")?.toInt() ?: 15,
            yearlyBookGoal = doc.long("yearlyBookGoal")?.toInt() ?: 12,
            readerTheme = doc.getString("readerTheme") ?: "Light",
            readerFont = doc.getString("readerFont") ?: "System",
            readerFontSize = doc.long("readerFontSize")?.toInt() ?: 18,
            readerLineSpacing = doc.getDouble("readerLineSpacing")?.toFloat() ?: 1.6f,
            readerHorizontalMargin = doc.long("readerHorizontalMargin")?.toInt() ?: 24,
            readingStyle = doc.getString("readingStyle") ?: "Scroll",
            textAlignment = doc.getString("textAlignment") ?: "Justified",
            appThemeMode = doc.getString("appThemeMode") ?: "System",
            dailyRemindersEnabled = doc.getBoolean("dailyRemindersEnabled") ?: false,
            reminderMinutesAfterMidnight = doc.long("reminderMinutesAfterMidnight")?.toInt(),
            updatedAtMillis = updatedAt,
            autoCloudSyncEnabled = doc.getBoolean("autoCloudSyncEnabled") ?: true,
            readerBrightness = doc.getDouble("readerBrightness")?.toFloat() ?: -1f,
            libraryShelvesJson = doc.getString("libraryShelvesJson") ?: "[]",
            libraryMembershipJson = doc.getString("libraryMembershipJson") ?: "{}",
            librarySortOrder = doc.getString("librarySortOrder") ?: "DateAdded",
            audioVoiceName = doc.getString("audioVoiceName") ?: ""
        ))
    }

    private suspend fun removeMigratedRemoteBookIds(userId: String) {
        remoteBookIdAliases.forEach { (oldId, localId) ->
            if (oldId == localId) return@forEach
            collection(userId, PROGRESS).document(oldId).delete().await()
            collection(userId, BOOKS).document(oldId).delete().await()
        }
    }

    private fun BookEntity.toCloudMap() = mapOf(
        "title" to title, "authors" to AuthorsJson.decode(authorsJson), "description" to description,
        "format" to format.name, "genre" to genre, "totalPages" to totalPages,
        "addedAtMillis" to addedAtMillis, "lastOpenedAtMillis" to lastOpenedAtMillis,
        "fileSizeBytes" to fileSizeBytes
    )
    private fun ReadingProgressEntity.toCloudMap() = mapOf(
        "bookId" to bookId, "locationJson" to locationJson, "currentPage" to currentPage,
        "totalPages" to totalPages, "progression" to progression, "updatedAtMillis" to updatedAtMillis
    )
    private fun ReadingSessionEntity.toCloudMap() = mapOf(
        "id" to id, "bookId" to bookId, "startedAtMillis" to startedAtMillis,
        "endedAtMillis" to endedAtMillis, "durationMillis" to durationMillis,
        "pagesRead" to pagesRead, "wordsRead" to wordsRead, "mode" to mode.name
    )
    private fun ReaderBookmarkEntity.toCloudMap() = mapOf(
        "id" to id, "bookId" to bookId, "locationJson" to locationJson,
        "chapterTitle" to chapterTitle, "excerpt" to excerpt, "createdAtMillis" to createdAtMillis
    )
    private fun UserPreferencesEntity.toCloudMap() = mapOf(
        "dailyGoalMinutes" to dailyGoalMinutes, "yearlyBookGoal" to yearlyBookGoal,
        "readerTheme" to readerTheme, "readerFont" to readerFont, "readerFontSize" to readerFontSize,
        "readerLineSpacing" to readerLineSpacing, "readerHorizontalMargin" to readerHorizontalMargin,
        "readingStyle" to readingStyle, "textAlignment" to textAlignment,
        "appThemeMode" to appThemeMode, "dailyRemindersEnabled" to dailyRemindersEnabled,
        "reminderMinutesAfterMidnight" to reminderMinutesAfterMidnight, "updatedAtMillis" to updatedAtMillis,
        "autoCloudSyncEnabled" to autoCloudSyncEnabled, "readerBrightness" to readerBrightness,
        "libraryShelvesJson" to libraryShelvesJson, "libraryMembershipJson" to libraryMembershipJson,
        "librarySortOrder" to librarySortOrder, "audioVoiceName" to audioVoiceName
    )

    private fun DocumentSnapshot.long(field: String): Long? = getLong(field)
    private fun collection(userId: String, path: String) = users.document(userId).collection(path)
    private val users get() = firestore.collection(USERS)

    private companion object {
        const val USERS = "users"
        const val BOOKS = "books"
        const val PROGRESS = "readingProgress"
        const val SESSIONS = "readingSessions"
        const val BOOKMARKS = "bookmarks"
        const val PREFERENCES = "preferences"
        const val CURRENT = "current"
        const val MAX_BATCH_WRITES = 450
    }
}
