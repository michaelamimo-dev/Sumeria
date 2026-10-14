package com.michaelamimo.sumeria.data.repository

import com.michaelamimo.sumeria.data.local.dao.ReaderBookmarkDao
import com.michaelamimo.sumeria.data.auth.CurrentProfileProvider
import com.michaelamimo.sumeria.data.local.entity.toDomain
import com.michaelamimo.sumeria.data.local.entity.toEntity
import com.michaelamimo.sumeria.domain.model.ReaderBookmark
import com.michaelamimo.sumeria.domain.repository.ReaderBookmarkRepository
import com.michaelamimo.sumeria.domain.repository.CloudSyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomReaderBookmarkRepository @Inject constructor(
    private val dao: ReaderBookmarkDao,
    private val profileProvider: CurrentProfileProvider,
    private val cloudSyncRepository: CloudSyncRepository
) : ReaderBookmarkRepository {
    override fun observeForBook(bookId: String): Flow<List<ReaderBookmark>> = dao.observeForBook(profileProvider.currentProfileId(), bookId).map { rows -> rows.map { it.toDomain() } }
    override suspend fun save(bookmark: ReaderBookmark) = dao.upsert(bookmark.toEntity(profileProvider.currentProfileId()))
    override suspend fun delete(bookmarkId: String) {
        val profileId = profileProvider.currentProfileId()
        dao.delete(profileId, bookmarkId)
        if (profileId != com.michaelamimo.sumeria.domain.model.UserPreferences.LOCAL_PROFILE_ID) {
            runCatching { cloudSyncRepository.deleteBookmark(profileId, bookmarkId) }
        }
    }
}
