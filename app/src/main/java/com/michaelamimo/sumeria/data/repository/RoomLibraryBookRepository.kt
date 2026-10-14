package com.michaelamimo.sumeria.data.repository

import com.michaelamimo.sumeria.data.local.dao.BookDao
import com.michaelamimo.sumeria.data.auth.CurrentProfileProvider
import com.michaelamimo.sumeria.data.local.entity.AuthorsJson
import com.michaelamimo.sumeria.data.local.entity.toDomain
import com.michaelamimo.sumeria.data.local.entity.toEntity
import com.michaelamimo.sumeria.domain.model.LibraryBook
import com.michaelamimo.sumeria.domain.repository.LibraryBookRepository
import com.michaelamimo.sumeria.domain.repository.CloudSyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomLibraryBookRepository @Inject constructor(
    private val dao: BookDao,
    private val profileProvider: CurrentProfileProvider,
    private val cloudSyncRepository: CloudSyncRepository
) : LibraryBookRepository {
    override fun observeAll(): Flow<List<LibraryBook>> = dao.observeAll(profileProvider.currentProfileId()).map { rows -> rows.map { it.toDomain() } }
    override fun observeById(bookId: String): Flow<LibraryBook?> = dao.observeById(profileProvider.currentProfileId(), bookId).map { it?.toDomain() }
    override suspend fun getById(bookId: String): LibraryBook? = dao.getById(profileProvider.currentProfileId(), bookId)?.toDomain()
    override suspend fun search(query: String): List<LibraryBook> = dao.search(profileProvider.currentProfileId(), query.trim()).map { it.toDomain() }
    override suspend fun upsert(book: LibraryBook) {
        val owner = profileProvider.currentProfileId()
        val matchingCloudRecord = dao.findByMetadata(owner, book.title, AuthorsJson.encode(book.authors), book.format.name)
        val localId = matchingCloudRecord?.takeIf { it.sourceUri == null }?.id ?: book.id
        dao.upsert(book.copy(id = localId).toEntity(owner))
    }
    override suspend fun delete(bookId: String) {
        val profileId = profileProvider.currentProfileId()
        dao.delete(profileId, bookId)
        if (profileId != com.michaelamimo.sumeria.domain.model.UserPreferences.LOCAL_PROFILE_ID) {
            runCatching { cloudSyncRepository.deleteBook(profileId, bookId) }
        }
    }
}
