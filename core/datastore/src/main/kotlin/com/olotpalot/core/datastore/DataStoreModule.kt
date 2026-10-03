package com.olotpalot.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.olotpalot.domain.repository.PreferencesRepository
import com.olotpalot.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreProvidesModule {
    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("user_preferences") }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataStoreBindsModule {
    @Binds abstract fun bindPreferences(impl: PreferencesRepositoryImpl): PreferencesRepository
    @Binds abstract fun bindUser(impl: LocalUserRepository): UserRepository
}
