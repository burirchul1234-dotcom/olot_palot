package com.olotpalot.data.mapper

import com.olotpalot.core.database.entity.AudioProgressEntity
import com.olotpalot.core.database.entity.BookmarkEntity
import com.olotpalot.core.database.entity.ChapterEntity
import com.olotpalot.core.database.entity.CollectionEntity
import com.olotpalot.core.database.entity.CommentEntity
import com.olotpalot.core.database.entity.ContentEntity
import com.olotpalot.core.database.entity.ContentWithMedia
import com.olotpalot.core.database.entity.MediaAssetEntity
import com.olotpalot.core.database.entity.ReadingProgressEntity
import com.olotpalot.core.database.entity.TaxonEntity
import com.olotpalot.core.database.entity.UserContentStateEntity
import com.olotpalot.core.database.entity.VideoProgressEntity
import com.olotpalot.domain.model.AccessPolicy
import com.olotpalot.domain.model.Author
import com.olotpalot.domain.model.Bookmark
import com.olotpalot.domain.model.Chapter
import com.olotpalot.domain.model.Collection
import com.olotpalot.domain.model.Comment
import com.olotpalot.domain.model.CommentStatus
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.model.ContentStats
import com.olotpalot.domain.model.Language
import com.olotpalot.domain.model.MediaAsset
import com.olotpalot.domain.model.MediaProgress
import com.olotpalot.domain.model.ReadingProgress
import com.olotpalot.domain.model.Taxon
import com.olotpalot.domain.model.TaxonKind
import com.olotpalot.domain.model.UserContentState

private const val LIST_SEP = "\u001F"
private const val PAIR_SEP = "|"

internal fun List<Taxon>.pack() = joinToString(LIST_SEP) { "${it.id}$PAIR_SEP${it.name}" }
internal fun String.unpackTaxa(kind: TaxonKind): List<Taxon> =
    if (isBlank()) emptyList() else split(LIST_SEP).mapNotNull { s ->
        val i = s.indexOf(PAIR_SEP); if (i <= 0) null else Taxon(s.substring(0, i), s.substring(i + 1), kind)
    }
internal fun List<String>.packStrings() = joinToString(LIST_SEP)
internal fun String.unpackStrings() = if (isBlank()) emptyList() else split(LIST_SEP)

fun ContentWithMedia.toDomain(): ContentItem = content.toDomain(media.map { it.toDomain() })

fun ContentEntity.toDomain(media: List<MediaAsset>): ContentItem = ContentItem(
    id = id,
    title = title,
    subtitle = subtitle,
    description = description,
    author = Author(authorId, authorName, authorBio, authorAvatarUrl),
    thumbnailUrl = thumbnailUrl,
    heroUrl = heroUrl,
    language = Language.fromCode(language),
    category = Taxon(categoryId, categoryName, TaxonKind.CATEGORY),
    genres = genres.unpackTaxa(TaxonKind.GENRE),
    moods = moods.unpackTaxa(TaxonKind.MOOD),
    tags = tags.unpackStrings(),
    createdAt = createdAt,
    publishedAt = publishedAt,
    updatedAt = updatedAt,
    isPublished = isPublished,
    isFeatured = isFeatured,
    isTrending = isTrending,
    readingMinutes = readingMinutes,
    audioDurationMs = audioDurationMs,
    videoDurationMs = videoDurationMs,
    pageCount = pageCount,
    access = runCatching { AccessPolicy.valueOf(access) }.getOrDefault(AccessPolicy.FREE),
    media = media,
    chapterCount = chapterCount,
    stats = ContentStats(views, likes, comments, shares, ratingAverage, ratingCount, completions),
)

fun ContentItem.toEntity() = ContentEntity(
    id = id, title = title, subtitle = subtitle, description = description,
    authorId = author.id, authorName = author.name, authorBio = author.bio, authorAvatarUrl = author.avatarUrl,
    thumbnailUrl = thumbnailUrl, heroUrl = heroUrl, language = language.code,
    categoryId = category.id, categoryName = category.name,
    genres = genres.pack(), moods = moods.pack(), tags = tags.packStrings(),
    createdAt = createdAt, publishedAt = publishedAt, updatedAt = updatedAt,
    isPublished = isPublished, isFeatured = isFeatured, isTrending = isTrending,
    readingMinutes = readingMinutes, audioDurationMs = audioDurationMs, videoDurationMs = videoDurationMs,
    pageCount = pageCount, access = access.name, chapterCount = chapterCount,
    views = stats.views, likes = stats.likes, comments = stats.comments, shares = stats.shares,
    ratingAverage = stats.ratingAverage, ratingCount = stats.ratingCount, completions = stats.completions,
)

fun MediaAssetEntity.toDomain() = MediaAsset(
    id, contentId, com.olotpalot.domain.model.MediaType.valueOf(type), url, durationMs, pageCount, mimeType, sizeBytes, thumbnailUrl, downloadable, chapterIndex,
)

fun MediaAsset.toEntity() = MediaAssetEntity(id, contentId, type.name, url, durationMs, pageCount, mimeType, sizeBytes, thumbnailUrl, downloadable, chapterIndex)

fun ChapterEntity.toDomain() = Chapter(id, contentId, chapterIndex, title, body, wordCount)
fun Chapter.toEntity() = ChapterEntity(id, contentId, index, title, body, wordCount)

fun TaxonEntity.toDomain() = Taxon(id, name, TaxonKind.valueOf(kind))
fun Taxon.toEntity(order: Int) = TaxonEntity(id, name, kind.name, order)

fun CollectionEntity.toDomain() = Collection(id, title, blurb, contentIds.unpackStrings())
fun Collection.toEntity(order: Int) = CollectionEntity(id, title, blurb, contentIds.packStrings(), order)

fun ReadingProgressEntity.toDomain() = ReadingProgress(contentId, chapterIndex, paragraphIndex, fraction, updatedAt)
fun ReadingProgress.toEntity() = ReadingProgressEntity(contentId, chapterIndex, paragraphIndex, fraction, updatedAt)
fun AudioProgressEntity.toDomain() = MediaProgress(contentId, assetId, positionMs, durationMs, updatedAt)
fun MediaProgress.toAudioEntity() = AudioProgressEntity(contentId, assetId, positionMs, durationMs, updatedAt)
fun VideoProgressEntity.toDomain() = MediaProgress(contentId, assetId, positionMs, durationMs, updatedAt)
fun MediaProgress.toVideoEntity() = VideoProgressEntity(contentId, assetId, positionMs, durationMs, updatedAt)

fun BookmarkEntity.toDomain() = Bookmark(id, contentId, chapterIndex, paragraphIndex, excerpt, note, createdAt)

fun CommentEntity.toDomain(currentUserId: String?) = Comment(
    id, contentId, userId, userName, body, createdAt, editedAt,
    runCatching { CommentStatus.valueOf(status) }.getOrDefault(CommentStatus.VISIBLE), parentId,
    isMine = currentUserId != null && currentUserId == userId,
)

fun UserContentStateEntity?.toDomain(
    id: String,
    reading: ReadingProgressEntity?,
    audio: AudioProgressEntity?,
    video: VideoProgressEntity?,
): UserContentState = UserContentState(
    contentId = id,
    isLiked = this?.liked ?: false,
    isFavourite = this?.favourite ?: false,
    isSavedForLater = this?.savedForLater ?: false,
    userRating = this?.rating,
    isDownloaded = this?.downloaded ?: false,
    isCompleted = this?.completed ?: false,
    reading = reading?.toDomain(),
    audio = audio?.toDomain(),
    video = video?.toDomain(),
    lastOpenedAt = this?.lastOpenedAt,
)
