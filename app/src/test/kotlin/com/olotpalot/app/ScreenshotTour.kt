package com.olotpalot.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToString
import androidx.test.core.app.ActivityScenario
import com.olotpalot.domain.repository.ContentRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import javax.inject.Inject

/**
 * Drives the real app (real Hilt graph, Room, DataStore, seed catalog) under
 * Robolectric's native renderer and writes a PNG of each screen to
 * app/build/screenshots. Not an assertion suite — a way to *see* the product
 * where no emulator exists.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [35], qualifiers = "w411dp-h914dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTour {

    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createEmptyComposeRule()

    @Inject lateinit var content: ContentRepository

    private val outDir = File("build/screenshots").apply { mkdirs() }
    private var shot = 0

    init {
        System.setProperty("robolectric.pixelCopyRenderMode", "hardware")
        org.robolectric.shadows.ShadowLog.stream = System.out
    }

    @Test
    fun tour() {
        hilt.inject()
        runBlocking { content.ensureSeeded() }
        // Robolectric has no ImageDecoder; make Coil decode with BitmapFactory in this JVM run only.
        val appCtx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        coil3.SingletonImageLoader.setSafe {
            coil3.ImageLoader.Builder(appCtx)
                .components { add(coil3.decode.BitmapFactoryDecoder.Factory()) }
                .build()
        }
        diagnoseImages()
        prewarmImages(appCtx)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.use {
            // ---- Home (Standard theme)
            waitForText("Trending now")
            settle(images = true)
            snap("home-top")
            scrollTo("Trending now"); settle()
            snap("home-trending")
            scrollTo("Browse by mood"); settle()
            snap("home-moods")
            scrollTo("Recently added"); settle()
            snap("home-recent")

            // ---- Discover
            t("Discover").performClick()
            waitForText("Collections"); settle(images = true)
            snap("discover")
            compose.onNode(hasSetTextAction()).performTextInput("rain")
            waitForText("Coffee, Rain, and Second Chances"); settle(images = true)
            snap("discover-search")
            runCatching {
                compose.onNodeWithContentDescription("Filters").performClick(); settle()
                snapRoot(lastRoot = true, name = "discover-filters")
                t("Show results").performClick(); settle()
            }
            compose.onNodeWithContentDescription("Clear search").performClick(); settle()

            // ---- Story detail
            goHome()
            all("Echoes of the Starlit Sea")[0].performClick()
            waitForText("Read Story"); settle(images = true, ms = 6_000)
            snap("story-hero")
            scrollTo("Contents"); settle()
            snap("story-contents")
            scrollTo("More like this"); settle(images = true)
            snap("story-comments-related")
            scrollTo("Favourite")
            t("Favourite").performClick()
            t("Later").performClick(); settle()

            // ---- Reader
            scrollTo("Read Story")
            t("Read Story").performClick()
            waitForText("The Keeper"); settle()
            snap("reader")
            scrollTo("Continue reading"); settle(ms = 1500)
            snap("reader-chapter-end")
            compose.onNodeWithContentDescription("Back").performClick(); settle(ms = 1500)

            // ---- Listen
            runCatching {
                scrollTo("Listen")
                t("Listen").performClick()
                waitForText("Listening"); settle(images = true, ms = 2500)
                snap("listen")
                compose.onNodeWithContentDescription("Back").performClick(); settle(ms = 1200)
            }

            // ---- Back home: continue strip + progress
            compose.onNodeWithContentDescription("Back").performClick()
            settle(); scrollTo("Olot Palot"); settle(images = true)
            snap("home-continue")

            // ---- Watch
            t("Watch").performClick()
            waitForText("Short films and stories with pictures"); settle(images = true)
            snap("watch")

            // ---- Library
            t("Library").performClick()
            waitForText("Saved for later"); settle(images = true)
            snap("library")

            // ---- Profile + themes
            t("Profile").performClick()
            waitForText("Appearance"); settle()
            snap("profile")
            compose.onNodeWithContentDescription("Dark theme").performClick(); settle()
            snap("profile-dark")
            goHome(); settle(images = true)
            snap("home-dark")
            t("Profile").performClick(); waitForText("Appearance"); settle()
            compose.onNodeWithContentDescription("Sunrise theme").performClick(); settle()
            goHome(); settle(images = true)
            snap("home-sunrise")
            t("Profile").performClick(); waitForText("Appearance"); settle()
            compose.onNodeWithContentDescription("Light theme").performClick(); settle()
            t("Discover").performClick(); waitForText("Collections"); settle(images = true)
            snap("discover-light")
            t("Profile").performClick(); waitForText("Appearance"); settle()
            compose.onNodeWithContentDescription("Standard theme").performClick(); settle()

            // ---- Bengali story
            t("Discover").performClick(); waitForText("Collections"); settle()
            all("নদীর ওপারে আলো")[0].performClick()
            waitForText("Read Story"); settle(images = true)
            snap("story-bengali")
            scrollTo("Read Story")
            t("Read Story").performClick()
            waitForText("ঘাট"); settle()
            snap("reader-bengali")
        }
    }

    private lateinit var scenario: ActivityScenario<MainActivity>

    private fun t(text: String) = compose.onNodeWithText(text, ignoreCase = true)

    /** Home restores its scroll position; bring it back to the masthead so the tour is deterministic. */
    private fun goHome() {
        t("Home").performClick()
        settle()
        scrollTo("Olot Palot")
        settle()
    }

    /** Lazy lists only compose what's visible, so scroll the list itself to the matching item. */
    private fun scrollTo(text: String) {
        compose.onAllNodes(hasScrollToNodeAction())[0].performScrollToNode(hasText(text, ignoreCase = true))
        compose.waitForIdle()
    }
    private fun all(text: String) = compose.onAllNodesWithText(text, ignoreCase = true)

    /** Load every catalog image into Coil's cache first, so screens render fully rather than mid-crossfade. */
    private fun prewarmImages(ctx: android.content.Context) = runBlocking {
        val items = content.observeAll().first()
        val urls = items.flatMap { listOf(it.thumbnailUrl, it.heroUrl) + it.media.mapNotNull { m -> m.thumbnailUrl } }.distinct()
        val loader = coil3.SingletonImageLoader.get(ctx)
        var ok = 0
        urls.map { url -> async { loader.execute(coil3.request.ImageRequest.Builder(ctx).data(url).build()) } }
            .forEach { if (it.await() is coil3.request.SuccessResult) ok++ }
        println("DIAG prewarmed $ok/${urls.size} images")
    }

    private fun diagnoseImages() {
        val url = "https://picsum.photos/seed/starlit/300/400"
        runCatching {
            val client = okhttp3.OkHttpClient()
            client.newCall(okhttp3.Request.Builder().url(url).build()).execute().use { r ->
                println("DIAG okhttp status=${r.code} type=${r.header("Content-Type")} bytes=${r.body?.bytes()?.size}")
            }
        }.onFailure { println("DIAG okhttp failed: $it") }
        runCatching {
            val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
            val loader = coil3.SingletonImageLoader.get(ctx)
            val result = runBlocking { loader.execute(coil3.request.ImageRequest.Builder(ctx).data(url).build()) }
            println("DIAG coil result=${result::class.simpleName} ${(result as? coil3.request.ErrorResult)?.throwable}")
        }.onFailure { println("DIAG coil threw: $it") }
    }

    private fun waitForText(text: String, timeoutMs: Long = 15_000) {
        try {
            compose.waitUntil(timeoutMs) { compose.onAllNodesWithText(text, substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: Throwable) {
            println("TREE-ON-TIMEOUT waiting for '$text':")
            runCatching { println(compose.onAllNodes(isRoot())[0].printToString(maxDepth = 12)) }.onFailure { println("no root: $it") }
            runCatching { snap("timeout-" + text.take(12).replace(' ', '_')) }.onFailure { println("SNAP-FAILED: $it") }
            throw e
        }
    }

    /** Let flows, animations and (optionally) network images land. */
    private fun settle(images: Boolean = false, ms: Long = if (images) 4_000 else 600) {
        val end = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < end) {
            Thread.sleep(100)
            org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(100))
            compose.waitForIdle()
        }
    }

    private fun snap(name: String) = snapRoot(lastRoot = false, name = name)

    private fun snapRoot(lastRoot: Boolean, name: String) {
        compose.waitForIdle()
        val bitmap: Bitmap = if (lastRoot) {
            // Dialog/sheet windows: Compose's own capture of the newest root.
            val roots = compose.onAllNodes(isRoot())
            val count = roots.fetchSemanticsNodes().size
            roots[count - 1].captureToImage().asAndroidBitmap()
        } else {
            // Draw twice: the first pass starts any pending image crossfades (Robolectric
            // renders no frames on its own), the second — after the fade duration — is kept.
            fun draw(): Bitmap {
                var bmp: Bitmap? = null
                scenario.onActivity { activity ->
                    val view = activity.window.decorView
                    val b = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                    view.draw(android.graphics.Canvas(b))
                    bmp = b
                }
                return bmp ?: error("no activity")
            }
            draw()
            settle(ms = 700)
            draw()
        }
        val file = File(outDir, "%02d-%s.png".format(++shot, name))
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        println("SCREENSHOT ${file.absolutePath}")
    }
}
