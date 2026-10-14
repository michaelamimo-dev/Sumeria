package com.michaelamimo.sumeria.data.sync

import android.util.Log
import com.michaelamimo.sumeria.auth.AuthRepository
import com.michaelamimo.sumeria.data.local.dao.BookDao
import com.michaelamimo.sumeria.data.local.dao.ReaderBookmarkDao
import com.michaelamimo.sumeria.data.local.dao.ReadingProgressDao
import com.michaelamimo.sumeria.data.local.dao.ReadingSessionDao
import com.michaelamimo.sumeria.data.local.dao.UserPreferencesDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSyncCoordinator @Inject constructor(
    private val authRepository: AuthRepository,
    private val cloudSyncRepository: com.michaelamimo.sumeria.domain.repository.CloudSyncRepository,
    private val books: BookDao,
    private val progress: ReadingProgressDao,
    private val sessions: ReadingSessionDao,
    private val bookmarks: ReaderBookmarkDao,
    private val preferences: UserPreferencesDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var started = false

    @OptIn(FlowPreview::class)
    fun start() {
        if (started) return
        started = true
        scope.launch {
            authRepository.observeUser().distinctUntilChangedBy { it?.id }.collectLatest { user ->
                if (user == null) return@collectLatest
                val autoSyncEnabled = preferences.get(user.id)?.autoCloudSyncEnabled ?: true
                if (autoSyncEnabled) {
                    try {
                        cloudSyncRepository.syncUser(user.id)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Log.w(TAG, "Initial cloud sync failed; local Room data remains available.", error)
                    }
                }
                supervisorScope {
                    combine(
                        books.observeAll(user.id),
                        progress.observeAll(user.id),
                        sessions.observeAll(user.id),
                        bookmarks.observeAll(user.id),
                        preferences.observe(user.id)
                    ) { _, _, _, _, userPreferences -> userPreferences?.autoCloudSyncEnabled ?: true }
                        .drop(1)
                        .debounce(SYNC_DEBOUNCE_MS)
                        .collectLatest { enabled ->
                            if (!enabled) return@collectLatest
                            try {
                                cloudSyncRepository.uploadLocalSnapshot(user.id)
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (error: Exception) {
                                Log.w(TAG, "Cloud sync upload failed; local changes will be retried after another update.", error)
                            }
                        }
                }
            }
        }
    }

    private companion object {
        const val TAG = "SumeriaCloudSync"
        const val SYNC_DEBOUNCE_MS = 1_500L
    }
}
