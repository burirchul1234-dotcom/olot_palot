package com.olotpalot.core.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * Catalog cache. Mirrors the remote `content` table one-to-one so a Supabase
 * row maps straight in. Taxonomy lists are stored as "id|name" strings joined
 * by a unit separator — small, read-mostly, and avoids three join tables for a
 * list the UI only ever renders whole.
 */
@Entity(
    tableName = "content",
    indices = [Index("publishedAt"), Index("isFeatured"), Index("categoryId"), Index("language")],
)
data class ContentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String?,
    val description: String,
    val authorId: String,
    val authorName: String,
    val authorBio: String?,
    val authorAvatarUrl: String?,
    val thumbnailUrl: String,
    val heroUrl: String,
    val language: String,
    val categoryId: String,
    val categoryName: String,
    val genres: String,
    val moods: String,
    val tags: String,
    val createdAt: Long,
    val publishedAt: Long,
    val updatedAt: Long,
    val isPublished: Boolean,
    val isFeatured: Boolean,
    val isTrending: Boolean,
    val readingMinutes: Int,
    val audioDurationMs: Long,
    val videoDurationMs: Long,
    val pageCount: Int,
    val access: String,
    val chapterCount: Int,
    val views: Long,
    val likes: Long,
    val comments: Long,
    val shares: Long,
    val ratingAverage: Float,
    val ratingCount: Int,
    val completions: Long,
)

@Entity(tableName = "media_assets", indices = [Index("contentId")])
data class MediaAssetEntity(
    @PrimaryKey val id: String,
    val contentId: String,
    val type: String,
    val url: String,
    val durationMs: Long,
    val pageCount: Int,
    val mimeType: String?,
    val sizeBytes: Long,
    val thumbnailUrl: String?,
    val downloadable: Boolean,
    val chapterIndex: Int?,
)

@Entity(tableName = "chapters", indices = [Index("contentId")])
data class ChapterEntity(
    @PrimaryKey val id: String,
    val contentId: String,
    val chapterIndex: Int,
    val title: String,
    val body: String,
    val wordCount: Int,
)

@Entity(tableName = "taxa")
data class TaxonEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String,
    val sortOrder: Int,
)

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val blurb: String,
    val contentIds: String,
    val sortOrder: Int,
)

data class ContentWithMedia(
    @Embedded val content: ContentEntity,
    @Relation(parentColumn = "id", entityColumn = "contentId") val media: List<MediaAssetEntity>,
)
