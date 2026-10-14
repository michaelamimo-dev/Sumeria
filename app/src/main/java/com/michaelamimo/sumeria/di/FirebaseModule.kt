package com.michaelamimo.sumeria.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.michaelamimo.sumeria.auth.AuthRepository
import com.michaelamimo.sumeria.data.auth.FirebaseAuthRepository
import com.michaelamimo.sumeria.data.sync.FirebaseCloudSyncRepository
import com.michaelamimo.sumeria.domain.repository.CloudSyncRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseServicesModule {
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class FirebaseBindingsModule {
    @Binds
    abstract fun bindAuthRepository(implementation: FirebaseAuthRepository): AuthRepository

    @Binds
    abstract fun bindCloudSyncRepository(implementation: FirebaseCloudSyncRepository): CloudSyncRepository
}
