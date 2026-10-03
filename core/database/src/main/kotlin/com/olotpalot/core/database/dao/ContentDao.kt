package com.olotpalot.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.olotpalot.core.database.entity.ChapterEntity
import com.olotpalot.core.database.entity.CollectionEntity
import com.olotpalot.core.database.entity.ContentEntity
import com.olotpalot.core.database.entity.ContentWithMedia
import com.olotpalot.core.database.entity.MediaAssetEntity
import com.olotpalot.core.database.entity.TaxonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {
    @Transaction
    @Query("SELECT * FROM content ORDER BY publishedAt DESC")
    fun observeAll(): Flow<List<ContentWithMedia>>

    @Transaction
    @Query("SELECT * FROM content WHERE id = :id")
    fun observeById(id: String): Flow<ContentWithMedia?>

    @Transaction
    @Query("SELECT * FROM content WHERE id IN (:ids)")
    fun observeByIds(ids: List<String>): Flow<List<ContentWithMedia>>

    @Transaction
    @Query("SELECT * FROM content WHERE id = :id")
    suspend fun getById(id: String): ContentWithMedia?

    @Transaction
    @Query("SELECT * FROM content")
    suspend fun getAll(): List<ContentWithMedia>

    @Query("SELECT COUNT(*) FROM content")
    suspend fun count(): Int

    @Upsert suspend fun upsertContent(items: List<ContentEntity>)
    @Upsert suspend fun upsertMedia(items: List<MediaAssetEntity>)
    @Upsert suspend fun upsertChapters(items: List<ChapterEntity>)
    @Upsert suspend fun upsertTaxa(items: List<TaxonEntity>)
    @Upsert suspend fun upsertCollections(items: List<CollectionEntity>)

    @Query("SELECT * FROM chapters WHERE contentId = :contentId ORDER BY chapterIndex")
    suspend fun chapters(contentId: String): List<ChapterEntity>

    @Query("SELECT * FROM taxa WHERE kind = :kind ORDER BY sortOrder")
    fun observeTaxa(kind: String): Flow<List<TaxonEntity>>

    @Query("SELECT * FROM collections ORDER BY sortOrder")
    suspend fun collections(): List<CollectionEntity>

    @Query("UPDATE content SET likes = MAX(0, likes + :delta) WHERE id = :id")
    suspend fun bumpLikes(id: String, delta: Int)

    @Query("UPDATE content SET views = views + 1 WHERE id = :id")
    suspend fun bumpViews(id: String)

    @Query("UPDATE content SET shares = shares + 1 WHERE id = :id")
    suspend fun bumpShares(id: String)

    @Query("UPDATE content SET comments = MAX(0, comments + :delta) WHERE id = :id")
    suspend fun bumpComments(id: String, delta: Int)

    @Query("UPDATE content SET completions = completions + 1 WHERE id = :id")
    suspend fun bumpCompletions(id: String)

    @Query("UPDATE content SET ratingAverage = :average, ratingCount = :count WHERE id = :id")
    suspend fun setRating(id: String, average: Float, count: Int)
}
