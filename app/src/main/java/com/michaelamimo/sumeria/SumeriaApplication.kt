package com.michaelamimo.sumeria

import android.app.Application
import com.michaelamimo.sumeria.data.sync.FirebaseSyncCoordinator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SumeriaApplication : Application() {
    @Inject lateinit var syncCoordinator: FirebaseSyncCoordinator

    override fun onCreate() {
        super.onCreate()
        syncCoordinator.start()
    }
}
