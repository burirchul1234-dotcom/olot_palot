package com.olotpalot.domain.model

/** Why the user may (or may not) open content right now. */
sealed interface AccessDecision {
    data object Granted : AccessDecision
    data class RequiresReward(val contentId: ContentId) : AccessDecision
    data class RequiresPremium(val contentId: ContentId) : AccessDecision
}

enum class RewardStatus { STARTED, GRANTED, FAILED, EXPIRED }

data class RewardTransaction(
    val id: String,
    val contentId: ContentId,
    val userId: String,
    val status: RewardStatus,
    val providerReference: String?,
    val createdAt: Long,
    val expiresAt: Long?,
)
