package com.olotpalot.data.analytics

import android.util.Log
import com.olotpalot.core.common.AnalyticsEvent
import com.olotpalot.core.common.AnalyticsTracker
import com.olotpalot.core.common.ApplicationScope
import com.olotpalot.core.common.Clock
import com.olotpalot.core.database.dao.AnalyticsDao
import com.olotpalot.core.database.entity.AnalyticsEventEntity
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Development tracker: logcat + a local ring of recent events. Swap for a vendor via DI. */
@Singleton
class LocalAnalyticsTracker @Inject constructor(
    private val dao: AnalyticsDao,
    private val scope: ApplicationScope,
    private val clock: Clock,
) : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) {
        Log.d("Analytics", "${event.name} ${event.params}")
        scope.scope.launch {
            runCatching {
                dao.insert(AnalyticsEventEntity(name = event.name, params = event.params.toString(), at = clock.now()))
                dao.prune(clock.now() - RETENTION_MS)
            }
        }
    }

    private companion object {
        const val RETENTION_MS = 14 * 24 * 3_600_000L
    }
}
