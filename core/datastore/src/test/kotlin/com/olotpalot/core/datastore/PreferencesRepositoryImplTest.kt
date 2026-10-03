package com.olotpalot.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import com.olotpalot.domain.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Bug 1 from the previous app: theme reset after restart. These tests write
 * through one DataStore instance, tear it down (simulating process death) and
 * read through a fresh instance on the same file.
 */
class PreferencesRepositoryImplTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun open(file: File, scope: CoroutineScope) =
        PreferencesRepositoryImpl(PreferenceDataStoreFactory.create(scope = scope) { file })

    private fun restartCycle(theme: ThemeMode) = runBlocking {
        val file = File(tmp.root, "prefs_${theme.name}.preferences_pb")

        val firstScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        open(file, firstScope).setTheme(theme)
        firstScope.cancel() // process killed

        val secondScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val restored = open(file, secondScope).preferences.first().theme
        secondScope.cancel()
        assertThat(restored).isEqualTo(theme)
    }

    @Test fun `standard survives restart`() = restartCycle(ThemeMode.STANDARD)
    @Test fun `dark survives process death`() = restartCycle(ThemeMode.DARK)
    @Test fun `light survives restart`() = restartCycle(ThemeMode.LIGHT)
    @Test fun `sunrise survives restart`() = restartCycle(ThemeMode.SUNRISE)

    @Test
    fun `defaults are sane and recent searches dedupe`() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val repo = open(File(tmp.root, "defaults.preferences_pb"), scope)
        val initial = repo.preferences.first()
        assertThat(initial.theme).isEqualTo(ThemeMode.STANDARD)
        assertThat(initial.fontScale).isEqualTo(1f)
        repo.addRecentSearch("rain")
        repo.addRecentSearch("harbor")
        repo.addRecentSearch("Rain")
        assertThat(repo.preferences.first().recentSearches).containsExactly("Rain", "harbor").inOrder()
        scope.cancel()
    }
}
