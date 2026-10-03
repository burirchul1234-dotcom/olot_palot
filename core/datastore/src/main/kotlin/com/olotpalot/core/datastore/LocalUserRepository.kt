package com.olotpalot.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.olotpalot.domain.model.LocalUser
import com.olotpalot.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Anonymous-first identity. A stable local id is minted once so likes,
 * comments and ratings have an owner before any account exists; signing in
 * later links this id to the remote user instead of discarding local state.
 */
@Singleton
class LocalUserRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) : UserRepository {
    private val idKey = stringPreferencesKey("local_user_id")
    private val nameKey = stringPreferencesKey("display_name")

    override val user: Flow<LocalUser> = store.data.map { p ->
        LocalUser(id = p[idKey] ?: PENDING, displayName = p[nameKey] ?: "Reader")
    }

    override suspend fun current(): LocalUser {
        val existing = user.first()
        if (existing.id != PENDING) return existing
        val id = "local-" + UUID.randomUUID().toString()
        store.edit { it[idKey] = id }
        return existing.copy(id = id)
    }

    private companion object {
        const val PENDING = "pending"
    }
}
