package com.olotpalot.data.remote

import com.olotpalot.domain.model.Chapter
import com.olotpalot.domain.model.Collection
import com.olotpalot.domain.model.Comment
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.model.Taxon

/** Everything the catalog needs in one fetch. */
data class CatalogSnapshot(
    val content: List<ContentItem>,
    val chapters: List<Chapter>,
    val taxa: List<Taxon>,
    val collections: List<Collection>,
    val comments: List<Comment>,
)

/**
 * The remote boundary. [SeedRemoteContentDataSource] fulfils it from bundled
 * data; a Supabase implementation fulfils it from PostgREST. Nothing above
 * the repository knows which one is bound.
 */
interface RemoteContentDataSource {
    suspend fun fetchCatalog(): CatalogSnapshot
}
