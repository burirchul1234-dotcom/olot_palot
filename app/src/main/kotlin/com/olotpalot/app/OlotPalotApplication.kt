package com.olotpalot.app

import android.app.Application
import com.olotpalot.core.common.AnalyticsEvent
import com.olotpalot.core.common.AnalyticsTracker
import com.olotpalot.core.common.ApplicationScope
import com.olotpalot.domain.repository.ContentRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class OlotPalotApplication : Application() {

    @Inject lateinit var appScope: ApplicationScope
    @Inject lateinit var content: ContentRepository
    @Inject lateinit var analytics: AnalyticsTracker

    override fun onCreate() {
        super.onCreate()
        // Seed the local catalog off the main thread; screens observe Room and fill in as rows land.
        appScope.scope.launch { content.ensureSeeded() }
        analytics.track(AnalyticsEvent.APP_OPENED)
    }
}
