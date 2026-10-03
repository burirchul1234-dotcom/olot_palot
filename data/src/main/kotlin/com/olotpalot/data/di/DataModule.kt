package com.olotpalot.data.di

import com.olotpalot.core.common.AnalyticsTracker
import com.olotpalot.core.common.AppDispatchers
import com.olotpalot.core.common.ApplicationScope
import com.olotpalot.core.common.Clock
import com.olotpalot.data.access.AccessRepositoryImpl
import com.olotpalot.data.access.MockAdProvider
import com.olotpalot.data.analytics.LocalAnalyticsTracker
import com.olotpalot.data.remote.RemoteContentDataSource
import com.olotpalot.data.repository.BookmarkRepositoryImpl
import com.olotpalot.data.repository.CommentRepositoryImpl
import com.olotpalot.data.repository.ContentRepositoryImpl
import com.olotpalot.data.repository.EngagementRepositoryImpl
import com.olotpalot.data.repository.ProgressRepositoryImpl
import com.olotpalot.data.seed.SeedRemoteContentDataSource
import com.olotpalot.domain.ranking.DeterministicRankingEngine
import com.olotpalot.domain.ranking.RankingEngine
import com.olotpalot.domain.repository.AccessRepository
import com.olotpalot.domain.repository.AdProvider
import com.olotpalot.domain.repository.BookmarkRepository
import com.olotpalot.domain.repository.CommentRepository
import com.olotpalot.domain.repository.ContentRepository
import com.olotpalot.domain.repository.EngagementRepository
import com.olotpalot.domain.repository.ProgressRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindsModule {
    @Binds abstract fun content(impl: ContentRepositoryImpl): ContentRepository
    @Binds abstract fun engagement(impl: EngagementRepositoryImpl): EngagementRepository
    @Binds abstract fun progress(impl: ProgressRepositoryImpl): ProgressRepository
    @Binds abstract fun bookmarks(impl: BookmarkRepositoryImpl): BookmarkRepository
    @Binds abstract fun comments(impl: CommentRepositoryImpl): CommentRepository
    @Binds abstract fun analytics(impl: LocalAnalyticsTracker): AnalyticsTracker
    @Binds abstract fun access(impl: AccessRepositoryImpl): AccessRepository
    @Binds abstract fun ads(impl: MockAdProvider): AdProvider

    /** Swap this single binding for the Supabase data source when credentials exist. */
    @Binds abstract fun remote(impl: SeedRemoteContentDataSource): RemoteContentDataSource
}

@Module
@InstallIn(SingletonComponent::class)
object DataProvidesModule {
    @Provides @Singleton fun clock(): Clock = Clock.System

    @Provides @Singleton fun ranking(): RankingEngine = DeterministicRankingEngine()

    @Provides @Singleton fun dispatchers(): AppDispatchers = object : AppDispatchers {
        override val io: CoroutineDispatcher = Dispatchers.IO
        override val default: CoroutineDispatcher = Dispatchers.Default
        override val main: CoroutineDispatcher = Dispatchers.Main
    }

    @Provides @Singleton fun applicationScope(dispatchers: AppDispatchers): ApplicationScope =
        ApplicationScope(CoroutineScope(SupervisorJob() + dispatchers.default))
}
