package com.olotpalot.data.access

import com.olotpalot.core.common.AppDispatchers
import com.olotpalot.core.common.AppError
import com.olotpalot.core.common.AppResult
import com.olotpalot.core.common.Clock
import com.olotpalot.core.database.dao.RewardDao
import com.olotpalot.core.database.entity.RewardUnlockEntity
import com.olotpalot.domain.model.AccessDecision
import com.olotpalot.domain.model.AccessPolicy
import com.olotpalot.domain.model.ContentId
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.model.RewardStatus
import com.olotpalot.domain.model.RewardTransaction
import com.olotpalot.domain.repository.AccessRepository
import com.olotpalot.domain.repository.AdProvider
import com.olotpalot.domain.repository.AdResult
import com.olotpalot.domain.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessRepositoryImpl @Inject constructor(
    private val rewardDao: RewardDao,
    private val adProvider: AdProvider,
    private val users: UserRepository,
    private val dispatchers: AppDispatchers,
    private val clock: Clock,
) : AccessRepository {

    override fun observeAccess(item: ContentItem): Flow<AccessDecision> = when (item.access) {
        AccessPolicy.FREE -> flowOf(AccessDecision.Granted)
        AccessPolicy.PREMIUM -> flowOf(AccessDecision.RequiresPremium(item.id))
        AccessPolicy.REWARDED -> rewardDao.observeGrants(item.id).map { grants ->
            val now = clock.now()
            if (grants.any { g -> g.expiresAt?.let { it > now } ?: true }) AccessDecision.Granted
            else AccessDecision.RequiresReward(item.id)
        }
    }

    /**
     * Explicit transaction: STARTED is persisted before the ad is shown, and the
     * grant is only written after the provider returns a reference. A crash
     * mid-ad leaves a STARTED row, never a phantom unlock.
     */
    override suspend fun unlockWithReward(id: ContentId): AppResult<RewardTransaction> = withContext(dispatchers.io) {
        val user = users.current()
        val txId = UUID.randomUUID().toString()
        val started = RewardUnlockEntity(txId, id, user.id, RewardStatus.STARTED.name, null, clock.now(), null)
        rewardDao.upsert(started)
        when (val result = adProvider.showRewarded(placement = "unlock:$id")) {
            is AdResult.Rewarded -> {
                val granted = started.copy(
                    status = RewardStatus.GRANTED.name,
                    providerReference = result.providerReference,
                    expiresAt = clock.now() + GRANT_DURATION_MS,
                )
                rewardDao.upsert(granted)
                AppResult.Success(RewardTransaction(txId, id, user.id, RewardStatus.GRANTED, result.providerReference, granted.createdAt, granted.expiresAt))
            }
            AdResult.Dismissed -> {
                rewardDao.upsert(started.copy(status = RewardStatus.FAILED.name))
                AppResult.Failure(AppError.Message("The ad was closed before it finished."))
            }
            is AdResult.Failed -> {
                rewardDao.upsert(started.copy(status = RewardStatus.FAILED.name))
                AppResult.Failure(AppError.Message(result.reason))
            }
        }
    }

    private companion object {
        /** Rewarded access is temporary by design: 7 days, then the user watches again. */
        const val GRANT_DURATION_MS = 7 * 24 * 3_600_000L
    }
}

/** Simulates a rewarded ad: a few seconds of "playback", then a verifiable reference. */
@Singleton
class MockAdProvider @Inject constructor() : AdProvider {
    override suspend fun showRewarded(placement: String): AdResult {
        delay(4_000)
        return AdResult.Rewarded(providerReference = "mock-" + UUID.randomUUID().toString().take(8))
    }
}
