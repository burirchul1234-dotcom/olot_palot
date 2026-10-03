package com.olotpalot.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.olotpalot.core.database.entity.AudioProgressEntity
import com.olotpalot.core.database.entity.ReadingProgressEntity
import com.olotpalot.core.database.entity.VideoProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Upsert suspend fun upsertReading(p: ReadingProgressEntity)
    @Upsert suspend fun upsertAudio(p: AudioProgressEntity)
    @Upsert suspend fun upsertVideo(p: VideoProgressEntity)

    @Query("SELECT * FROM reading_progress WHERE contentId = :id") suspend fun reading(id: String): ReadingProgressEntity?
    @Query("SELECT * FROM audio_progress WHERE contentId = :id") suspend fun audio(id: String): AudioProgressEntity?
    @Query("SELECT * FROM video_progress WHERE contentId = :id") suspend fun video(id: String): VideoProgressEntity?

    @Query("SELECT * FROM reading_progress") fun observeReading(): Flow<List<ReadingProgressEntity>>
    @Query("SELECT * FROM audio_progress") fun observeAudio(): Flow<List<AudioProgressEntity>>
    @Query("SELECT * FROM video_progress") fun observeVideo(): Flow<List<VideoProgressEntity>>

    @Query("SELECT * FROM reading_progress WHERE contentId = :id") fun observeReading(id: String): Flow<ReadingProgressEntity?>
    @Query("SELECT * FROM audio_progress WHERE contentId = :id") fun observeAudio(id: String): Flow<AudioProgressEntity?>
    @Query("SELECT * FROM video_progress WHERE contentId = :id") fun observeVideo(id: String): Flow<VideoProgressEntity?>

    @Query("DELETE FROM reading_progress WHERE contentId = :id") suspend fun clearReading(id: String)
    @Query("DELETE FROM audio_progress WHERE contentId = :id") suspend fun clearAudio(id: String)
    @Query("DELETE FROM video_progress WHERE contentId = :id") suspend fun clearVideo(id: String)

    @Query("SELECT COALESCE(SUM(positionMs), 0) FROM audio_progress") fun observeListenedMs(): Flow<Long>
}
