package com.olotpalot.domain.usecase

import com.olotpalot.core.common.AnalyticsEvent
import com.olotpalot.core.common.AnalyticsTracker
import com.olotpalot.core.common.AppResult
import com.olotpalot.domain.model.ContentId
import com.olotpalot.domain.model.RewardTransaction
import com.olotpalot.domain.repository.AccessRepository
import javax.inject.Inject

class UnlockWithRewardUseCase @Inject constructor(
    private val access: AccessRepository,
    private val analytics: AnalyticsTracker,
) {
    suspend operator fun invoke(id: ContentId): AppResult<RewardTransaction> {
        analytics.track(AnalyticsEvent.REWARD_STARTED, "contentId" to id)
        val result = access.unlockWithReward(id)
        when (result) {
            is AppResult.Success -> analytics.track(AnalyticsEvent.REWARD_GRANTED, "contentId" to id, "ref" to result.value.providerReference)
            is AppResult.Failure -> analytics.track(AnalyticsEvent.REWARD_FAILED, "contentId" to id)
        }
        return result
    }
}
