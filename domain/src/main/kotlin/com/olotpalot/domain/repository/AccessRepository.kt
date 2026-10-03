package com.olotpalot.domain.repository

import com.olotpalot.core.common.AppResult
import com.olotpalot.domain.model.AccessDecision
import com.olotpalot.domain.model.ContentId
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.model.RewardTransaction
import kotlinx.coroutines.flow.Flow

/**
 * Evaluates an item's access policy against the user's entitlements.
 * FREE → Granted. REWARDED → Granted while an unexpired reward exists.
 * PREMIUM → RequiresPremium until a subscription/purchase repository says otherwise.
 */
interface AccessRepository {
    fun observeAccess(item: ContentItem): Flow<AccessDecision>
    /** Runs the full reward flow: start → ad → verify → grant. Never unlocks from an unverified callback. */
    suspend fun unlockWithReward(id: ContentId): AppResult<RewardTransaction>
}

/** Vendor-agnostic ad boundary. Development binds [MockAdProvider]; production binds a real SDK. */
interface AdProvider {
    suspend fun showRewarded(placement: String): AdResult
}

sealed interface AdResult {
    data class Rewarded(val providerReference: String) : AdResult
    data object Dismissed : AdResult
    data class Failed(val reason: String) : AdResult
}
