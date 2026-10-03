package com.olotpalot.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.olotpalot.core.database.dao.AnalyticsDao
import com.olotpalot.core.database.dao.BookmarkDao
import com.olotpalot.core.database.dao.CommentDao
import com.olotpalot.core.database.dao.ContentDao
import com.olotpalot.core.database.dao.DownloadDao
import com.olotpalot.core.database.dao.EngagementDao
import com.olotpalot.core.database.dao.ProgressDao
import com.olotpalot.core.database.dao.RewardDao
import com.olotpalot.core.database.entity.AnalyticsEventEntity
import com.olotpalot.core.database.entity.AudioProgressEntity
import com.olotpalot.core.database.entity.BookmarkEntity
import com.olotpalot.core.database.entity.ChapterEntity
import com.olotpalot.core.database.entity.CollectionEntity
import com.olotpalot.core.database.entity.CommentEntity
import com.olotpalot.core.database.entity.ContentEntity
import com.olotpalot.core.database.entity.DownloadEntity
import com.olotpalot.core.database.entity.EngagementEventEntity
import com.olotpalot.core.database.entity.HistoryEntity
import com.olotpalot.core.database.entity.MediaAssetEntity
import com.olotpalot.core.database.entity.ReadingProgressEntity
import com.olotpalot.core.database.entity.RewardUnlockEntity
import com.olotpalot.core.database.entity.TaxonEntity
import com.olotpalot.core.database.entity.UserContentStateEntity
import com.olotpalot.core.database.entity.VideoProgressEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [
        ContentEntity::class, MediaAssetEntity::class, ChapterEntity::class, TaxonEntity::class, CollectionEntity::class,
        UserContentStateEntity::class, ReadingProgressEntity::class, AudioProgressEntity::class, VideoProgressEntity::class,
        BookmarkEntity::class, CommentEntity::class, EngagementEventEntity::class, HistoryEntity::class,
        AnalyticsEventEntity::class, RewardUnlockEntity::class, DownloadEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class OlotPalotDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
    abstract fun engagementDao(): EngagementDao
    abstract fun progressDao(): ProgressDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun commentDao(): CommentDao
    abstract fun analyticsDao(): AnalyticsDao
    abstract fun rewardDao(): RewardDao
    abstract fun downloadDao(): DownloadDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OlotPalotDatabase =
        Room.databaseBuilder(context, OlotPalotDatabase::class.java, "olotpalot.db")
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()

    @Provides fun contentDao(db: OlotPalotDatabase) = db.contentDao()
    @Provides fun engagementDao(db: OlotPalotDatabase) = db.engagementDao()
    @Provides fun progressDao(db: OlotPalotDatabase) = db.progressDao()
    @Provides fun bookmarkDao(db: OlotPalotDatabase) = db.bookmarkDao()
    @Provides fun commentDao(db: OlotPalotDatabase) = db.commentDao()
    @Provides fun analyticsDao(db: OlotPalotDatabase) = db.analyticsDao()
    @Provides fun rewardDao(db: OlotPalotDatabase) = db.rewardDao()
    @Provides fun downloadDao(db: OlotPalotDatabase) = db.downloadDao()
}
