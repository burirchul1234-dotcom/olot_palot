package com.olotpalot.domain.usecase

import com.olotpalot.domain.model.LibrarySnapshot
import com.olotpalot.domain.repository.BookmarkRepository
import com.olotpalot.domain.repository.EngagementRepository
import com.olotpalot.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveLibraryUseCase @Inject constructor(
    private val progress: ProgressRepository,
    private val engagement: EngagementRepository,
    private val bookmarks: BookmarkRepository,
) {
    operator fun invoke(): Flow<LibrarySnapshot> = combine(
        progress.observeContinue(limit = 20),
        engagement.observeSavedForLater(),
        engagement.observeFavourites(),
        bookmarks.observeAll(),
        combine(engagement.observeHistory(), engagement.observeCompleted()) { h, c -> h to c },
    ) { cont, saved, favs, marks, (history, completed) ->
        LibrarySnapshot(
            continueItems = cont,
            savedForLater = saved,
            favourites = favs,
            bookmarks = marks,
            history = history,
            completed = completed,
            downloaded = emptyList(),
        )
    }
}
