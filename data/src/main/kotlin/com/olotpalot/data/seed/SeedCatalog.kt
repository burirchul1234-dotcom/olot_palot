package com.olotpalot.data.seed

import com.olotpalot.core.common.Clock
import com.olotpalot.data.remote.CatalogSnapshot
import com.olotpalot.data.remote.RemoteContentDataSource
import com.olotpalot.domain.model.AccessPolicy
import com.olotpalot.domain.model.Author
import com.olotpalot.domain.model.Chapter
import com.olotpalot.domain.model.Collection
import com.olotpalot.domain.model.Comment
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.model.ContentStats
import com.olotpalot.domain.model.Language
import com.olotpalot.domain.model.MediaAsset
import com.olotpalot.domain.model.MediaType
import com.olotpalot.domain.model.Taxon
import com.olotpalot.domain.model.TaxonKind
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bundled development catalog. Shapes are identical to what the backend will
 * return; only the source differs. Media URLs point at freely licensed sample
 * files (Blender Foundation films, ExoPlayer test audio) and picsum artwork so
 * the app is fully usable before any CMS exists.
 */
@Singleton
class SeedRemoteContentDataSource @Inject constructor(private val clock: Clock) : RemoteContentDataSource {
    override suspend fun fetchCatalog(): CatalogSnapshot = SeedCatalog.snapshot(clock.now())
}

object SeedTaxa {
    private fun t(id: String, name: String, kind: TaxonKind) = Taxon(id, name, kind)

    val fiction = t("cat-fiction", "Fiction", TaxonKind.CATEGORY)
    val nonfiction = t("cat-nonfiction", "Nonfiction", TaxonKind.CATEGORY)
    val audioDrama = t("cat-audio-drama", "Audio drama", TaxonKind.CATEGORY)
    val shortFilm = t("cat-short-film", "Short film", TaxonKind.CATEGORY)
    val categories = listOf(fiction, nonfiction, audioDrama, shortFilm)

    val literary = t("g-literary", "Literary", TaxonKind.GENRE)
    val noir = t("g-noir", "Noir", TaxonKind.GENRE)
    val fantasy = t("g-fantasy", "Fantasy", TaxonKind.GENRE)
    val adventure = t("g-adventure", "Adventure", TaxonKind.GENRE)
    val romance = t("g-romance", "Romance", TaxonKind.GENRE)
    val memoir = t("g-memoir", "Memoir", TaxonKind.GENRE)
    val essay = t("g-essay", "Essay", TaxonKind.GENRE)
    val mystery = t("g-mystery", "Mystery", TaxonKind.GENRE)
    val scifi = t("g-scifi", "Science fiction", TaxonKind.GENRE)
    val genres = listOf(literary, noir, fantasy, adventure, romance, memoir, essay, mystery, scifi)

    val calm = t("m-calm", "Calm", TaxonKind.MOOD)
    val wistful = t("m-wistful", "Wistful", TaxonKind.MOOD)
    val tense = t("m-tense", "Tense", TaxonKind.MOOD)
    val wonder = t("m-wonder", "Wonder", TaxonKind.MOOD)
    val warm = t("m-warm", "Warm", TaxonKind.MOOD)
    val hopeful = t("m-hopeful", "Hopeful", TaxonKind.MOOD)
    val nocturnal = t("m-nocturnal", "Nocturnal", TaxonKind.MOOD)
    val reflective = t("m-reflective", "Reflective", TaxonKind.MOOD)
    val melancholy = t("m-melancholy", "Melancholy", TaxonKind.MOOD)
    val epic = t("m-epic", "Epic", TaxonKind.MOOD)
    val moods = listOf(calm, wistful, tense, wonder, warm, hopeful, nocturnal, reflective, melancholy, epic)

    val all = categories + genres + moods
}

object SeedAuthors {
    val mira = Author("au-mira-sen", "Mira Sen", "Writes about coastlines, memory, and the people who stay behind.", "https://i.pravatar.cc/120?img=47")
    val dev = Author("au-dev-roy", "Dev Anand Roy", "Noir, neon and the long night shift.", "https://i.pravatar.cc/120?img=12")
    val ines = Author("au-ines-varga", "Ines Varga", "Fables for adults who still look up.", "https://i.pravatar.cc/120?img=32")
    val tariq = Author("au-tariq-haddad", "Tariq Haddad", "Desert stories, told slowly.", "https://i.pravatar.cc/120?img=59")
    val jonah = Author("au-jonah-pike", "Jonah Pike", "Small rooms, big weather, second chances.", "https://i.pravatar.cc/120?img=15")
    val ananya = Author("au-ananya", "অনন্যা চক্রবর্তী", "নদী, গ্রাম আর ফিরে আসার গল্প।", "https://i.pravatar.cc/120?img=44")
    val soumya = Author("au-soumya", "সৌম্য বসু", "শহরের রাত আর হারিয়ে যাওয়া জিনিসের লেখক।", "https://i.pravatar.cc/120?img=68")
    val studio = Author("au-harbour-studio", "Harbour Films", "A small documentary collective.", "https://i.pravatar.cc/120?img=5")
}

object SeedMedia {
    const val AUDIO_A = "https://storage.googleapis.com/exoplayer-test-media-0/play.mp3"
    const val AUDIO_B = "https://storage.googleapis.com/exoplayer-test-media-0/Jazz_In_Paris.mp3"
    private const val GTV = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample"
    const val SINTEL = "$GTV/Sintel.mp4"
    const val SINTEL_THUMB = "https://picsum.photos/seed/sintel-still/1200/675"
    const val TEARS = "$GTV/TearsOfSteel.mp4"
    const val TEARS_THUMB = "https://picsum.photos/seed/tears-still/1200/675"
    const val ELEPHANTS = "$GTV/ElephantsDream.mp4"
    const val ELEPHANTS_THUMB = "https://picsum.photos/seed/elephants-still/1200/675"
    const val BUNNY = "$GTV/BigBuckBunny.mp4"
    const val BUNNY_THUMB = "https://picsum.photos/seed/bunny-still/1200/675"
    const val FUN = "$GTV/ForBiggerFun.mp4"
    const val FUN_THUMB = "https://picsum.photos/seed/harbour-still/1200/675"
    const val ESCAPES = "$GTV/ForBiggerEscapes.mp4"
    const val ESCAPES_THUMB = "https://picsum.photos/seed/paperboats-still/1200/675"

    fun thumb(seed: String) = "https://picsum.photos/seed/$seed/600/800"
    fun hero(seed: String) = "https://picsum.photos/seed/$seed/1200/1500"
}

object SeedCatalog {
    private const val DAY = 86_400_000L
    private const val MIN = 60_000L

    private class Builder(val now: Long) {
        val content = ArrayList<ContentItem>()
        val chapters = ArrayList<Chapter>()
        val comments = ArrayList<Comment>()

        fun story(
            id: String,
            title: String,
            subtitle: String?,
            description: String,
            author: Author,
            seed: String,
            language: Language = Language.ENGLISH,
            category: Taxon = SeedTaxa.fiction,
            genres: List<Taxon>,
            moods: List<Taxon>,
            tags: List<String>,
            daysAgo: Int,
            featured: Boolean = false,
            trending: Boolean = false,
            access: AccessPolicy = AccessPolicy.FREE,
            text: List<Pair<String, String>> = emptyList(),
            audio: Boolean = false,
            audioMinutes: Int = 0,
            video: Pair<String, String>? = null,
            videoMinutes: Int = 0,
            thumbnail: String = SeedMedia.thumb(seed),
            hero: String = SeedMedia.hero(seed),
            accent: Long = 0xFFB5432B,
            stats: ContentStats,
        ) {
            val published = now - daysAgo * DAY - (id.hashCode().mod(7)) * 3_600_000L
            val media = ArrayList<MediaAsset>()
            val chapterList = text.mapIndexed { i, (t, body) ->
                Chapter("$id-ch${i + 1}", id, i, t, body.trimIndent().trim(), body.split(Regex("\\s+")).size)
            }
            if (audio) {
                val perChapter = if (chapterList.isNotEmpty()) audioMinutes * MIN / chapterList.size else audioMinutes * MIN
                if (chapterList.isEmpty()) {
                    media += MediaAsset("$id-audio", id, MediaType.AUDIO, SeedMedia.AUDIO_B, audioMinutes * MIN, mimeType = "audio/mpeg", sizeBytes = 2_400_000)
                } else {
                    chapterList.forEachIndexed { i, _ ->
                        media += MediaAsset("$id-audio-${i + 1}", id, MediaType.AUDIO, if (i % 2 == 0) SeedMedia.AUDIO_A else SeedMedia.AUDIO_B, perChapter, mimeType = "audio/mpeg", sizeBytes = 1_800_000, chapterIndex = i)
                    }
                }
            }
            if (video != null) {
                media += MediaAsset("$id-video", id, MediaType.VIDEO, video.first, videoMinutes * MIN, mimeType = "video/mp4", sizeBytes = 120_000_000, thumbnailUrl = video.second)
            }
            val words = chapterList.sumOf { it.wordCount }
            content += ContentItem(
                id = id, title = title, subtitle = subtitle, description = description.trimIndent().trim(), author = author,
                thumbnailUrl = thumbnail, heroUrl = hero, language = language, category = category,
                genres = genres, moods = moods, tags = tags,
                createdAt = published - DAY, publishedAt = published, updatedAt = published,
                isPublished = true, isFeatured = featured, isTrending = trending,
                readingMinutes = if (words > 0) maxOf(3, words / 190) else 0,
                audioDurationMs = if (audio) audioMinutes * MIN else 0,
                videoDurationMs = if (video != null) videoMinutes * MIN else 0,
                pageCount = if (words > 0) maxOf(1, words / 300) else 0,
                access = access, media = media, chapterCount = chapterList.size, stats = stats,
                accentArgb = accent.toInt(),
            )
            chapters += chapterList
        }

        fun comment(contentId: String, who: String, body: String, hoursAgo: Int, n: Int) {
            comments += Comment(
                id = "seed-$contentId-$n", contentId = contentId, userId = "seed-user-${who.hashCode()}", userName = who,
                body = body, createdAt = now - hoursAgo * 3_600_000L,
            )
        }
    }

    fun snapshot(now: Long): CatalogSnapshot {
        val b = Builder(now)
        with(b) {
            story(
                id = "starlit-sea", accent = 0xFF2F4F5F, title = "Echoes of the Starlit Sea", subtitle = "A lighthouse, a radio, and the last voice on the frequency",
                description = """
                    On an island that the ferries forgot, Noor keeps a lighthouse that no ship needs and a radio that nobody answers — until, one night, someone does.
                    A story about signals, tides, and the stubborn habit of hoping.
                """,
                author = SeedAuthors.mira, seed = "starlit", genres = listOf(SeedTaxa.literary, SeedTaxa.mystery), moods = listOf(SeedTaxa.wistful, SeedTaxa.calm),
                tags = listOf("sea", "lighthouse", "radio", "island", "solitude"), daysAgo = 3, featured = true, trending = true,
                text = SeedTextEn.starlitSea, audio = true, audioMinutes = 38, video = SeedMedia.SINTEL to SeedMedia.SINTEL_THUMB, videoMinutes = 15,
                stats = ContentStats(views = 18_420, likes = 2_310, comments = 4, shares = 310, ratingAverage = 4.7f, ratingCount = 412, completions = 6_100),
            )
            story(
                id = "neon-harbor", accent = 0xFFB23A6E, title = "Midnight at Neon Harbor", subtitle = "Every light in the city is lying to someone",
                description = """
                    Detective Ira Bose has one night to find out why a dockside cold-storage unit is warm — and why the man who reported it no longer exists.
                    Rain-slick noir with a harbour full of secrets.
                """,
                author = SeedAuthors.dev, seed = "neonharbor", genres = listOf(SeedTaxa.noir, SeedTaxa.mystery), moods = listOf(SeedTaxa.tense, SeedTaxa.nocturnal),
                tags = listOf("detective", "city", "rain", "night", "harbour"), daysAgo = 9, trending = true,
                text = SeedTextEn.neonHarbor, audio = true, audioMinutes = 44, video = SeedMedia.TEARS to SeedMedia.TEARS_THUMB, videoMinutes = 12,
                stats = ContentStats(views = 12_980, likes = 1_540, comments = 3, shares = 190, ratingAverage = 4.4f, ratingCount = 288, completions = 3_900),
            )
            story(
                id = "clockwork-forest", accent = 0xFF3F6B3A, title = "Whispers of the Clockwork Forest", subtitle = "Where the trees keep time and the birds keep secrets",
                description = """
                    When the great forest's central spring begins to slow, a young tuner named Pell must walk to its heart — past owls that tick and rivers that chime — to wind it before winter stops altogether.
                """,
                author = SeedAuthors.ines, seed = "clockwork", genres = listOf(SeedTaxa.fantasy), moods = listOf(SeedTaxa.wonder, SeedTaxa.calm),
                tags = listOf("forest", "clockwork", "fable", "journey"), daysAgo = 21,
                text = SeedTextEn.clockworkForest, audio = true, audioMinutes = 31,
                stats = ContentStats(views = 9_120, likes = 1_205, comments = 2, shares = 140, ratingAverage = 4.8f, ratingCount = 201, completions = 3_300),
            )
            story(
                id = "crimson-dune", accent = 0xFFB5432B, title = "Shadows Beneath the Crimson Dune", subtitle = "The desert remembers what the map forgot",
                description = """
                    A caravan guide, a cartographer who lies, and a dune that moves against the wind. Tariq Haddad's slow-burning adventure across a desert that is older than its name.
                """,
                author = SeedAuthors.tariq, seed = "crimsondune", genres = listOf(SeedTaxa.adventure), moods = listOf(SeedTaxa.tense, SeedTaxa.epic),
                tags = listOf("desert", "caravan", "map", "sand"), daysAgo = 34, access = AccessPolicy.REWARDED,
                text = SeedTextEn.crimsonDune, audio = true, audioMinutes = 52, video = SeedMedia.ELEPHANTS to SeedMedia.ELEPHANTS_THUMB, videoMinutes = 11,
                stats = ContentStats(views = 7_450, likes = 820, comments = 1, shares = 96, ratingAverage = 4.2f, ratingCount = 150, completions = 2_010),
            )
            story(
                id = "winter-garden", accent = 0xFF4A6A8A, title = "The Alchemist's Winter Garden", subtitle = "Nothing grows here. Everything changes.",
                description = """
                    In a walled garden where frost is cultivated like fruit, an apprentice learns that transformation is mostly patience — and that her teacher has been turning something other than lead.
                """,
                author = SeedAuthors.ines, seed = "wintergarden", genres = listOf(SeedTaxa.fantasy, SeedTaxa.literary), moods = listOf(SeedTaxa.wonder, SeedTaxa.melancholy),
                tags = listOf("alchemy", "winter", "garden", "apprentice"), daysAgo = 60,
                text = SeedTextEn.winterGarden, audio = true, audioMinutes = 29,
                stats = ContentStats(views = 5_620, likes = 760, comments = 2, shares = 71, ratingAverage = 4.6f, ratingCount = 133, completions = 1_900),
            )
            story(
                id = "coffee-rain", accent = 0xFF8A5A3C, title = "Coffee, Rain, and Second Chances", subtitle = "A café, a storm, and twelve years of unsaid things",
                description = """
                    The power goes out at Café Monsoon with two customers inside. One of them left town twelve years ago without a word. The other has been pouring coffee here ever since.
                """,
                author = SeedAuthors.jonah, seed = "coffeerain", genres = listOf(SeedTaxa.romance, SeedTaxa.literary), moods = listOf(SeedTaxa.warm, SeedTaxa.hopeful),
                tags = listOf("café", "rain", "reunion", "love"), daysAgo = 5, trending = true,
                text = SeedTextEn.coffeeRain, audio = true, audioMinutes = 22,
                stats = ContentStats(views = 15_300, likes = 2_980, comments = 3, shares = 420, ratingAverage = 4.5f, ratingCount = 510, completions = 8_200),
            )
            story(
                id = "unfinished-chronicles", accent = 0xFF6B5E4A, title = "Unfinished Chronicles", subtitle = "Notes toward stories I never wrote",
                description = """
                    Mira Sen opens her notebooks: openings without endings, characters who wandered off, and the quiet reasons some stories are better left half-told.
                """,
                author = SeedAuthors.mira, seed = "unfinished", category = SeedTaxa.nonfiction, genres = listOf(SeedTaxa.essay, SeedTaxa.memoir), moods = listOf(SeedTaxa.reflective),
                tags = listOf("writing", "notebooks", "craft", "essay"), daysAgo = 14,
                text = SeedTextEn.unfinishedChronicles,
                stats = ContentStats(views = 3_210, likes = 540, comments = 1, shares = 88, ratingAverage = 4.3f, ratingCount = 77, completions = 1_100),
            )
            story(
                id = "nodir-opare", accent = 0xFF2E7D6B, title = "নদীর ওপারে আলো", subtitle = "ফিরে আসার একটি গল্প",
                description = """
                    বারো বছর পর তিতলি গ্রামে ফেরে। নদী আছে, ঘাট আছে, কিন্তু যে আলোটা ওপারে জ্বলত সেটা কে জ্বালাত, সে প্রশ্নের উত্তর এখনও কেউ দেয়নি।
                """,
                author = SeedAuthors.ananya, seed = "nodir", language = Language.BENGALI, genres = listOf(SeedTaxa.literary), moods = listOf(SeedTaxa.hopeful, SeedTaxa.wistful),
                tags = listOf("নদী", "গ্রাম", "ফেরা", "river", "village"), daysAgo = 7, trending = true,
                text = SeedTextBn.nodirOpare, audio = true, audioMinutes = 26,
                stats = ContentStats(views = 8_800, likes = 1_410, comments = 2, shares = 230, ratingAverage = 4.6f, ratingCount = 190, completions = 3_000),
            )
            story(
                id = "shesh-tram", accent = 0xFF9A6A1F, title = "শহরের শেষ ট্রাম", subtitle = "একটি রাত, একটি রুট, একটি শহর",
                description = """
                    কলকাতার শেষ ট্রামে চড়ে এক রাতের যাত্রা। সৌম্য বসুর স্মৃতিকথা — যা হারিয়ে যাচ্ছে, আর যা থেকে যায়।
                """,
                author = SeedAuthors.soumya, seed = "tram", language = Language.BENGALI, category = SeedTaxa.nonfiction, genres = listOf(SeedTaxa.memoir, SeedTaxa.essay), moods = listOf(SeedTaxa.nocturnal, SeedTaxa.reflective),
                tags = listOf("কলকাতা", "ট্রাম", "রাত", "kolkata", "tram"), daysAgo = 2,
                text = SeedTextBn.sheshTram, video = SeedMedia.BUNNY to SeedMedia.BUNNY_THUMB, videoMinutes = 10,
                stats = ContentStats(views = 2_140, likes = 380, comments = 1, shares = 60, ratingAverage = 4.4f, ratingCount = 41, completions = 600),
            )
            story(
                id = "small-harbour", accent = 0xFF3E6B7A, title = "Field Notes from a Small Harbour", subtitle = "A short film",
                description = """
                    One tide, four boats, and the people who wait for them. Shot over a single morning on a harbour wall.
                """,
                author = SeedAuthors.studio, seed = "harbourfilm", category = SeedTaxa.shortFilm, genres = listOf(SeedTaxa.memoir), moods = listOf(SeedTaxa.calm, SeedTaxa.reflective),
                tags = listOf("documentary", "harbour", "sea", "short"), daysAgo = 1,
                video = SeedMedia.FUN to SeedMedia.FUN_THUMB, videoMinutes = 1, thumbnail = SeedMedia.thumb("harbourfilm"), hero = SeedMedia.hero("harbourfilm"),
                stats = ContentStats(views = 1_050, likes = 140, comments = 0, shares = 22, ratingAverage = 4.1f, ratingCount = 19, completions = 700),
            )
            story(
                id = "night-frequencies", accent = 0xFF4B3F8C, title = "Night Frequencies", subtitle = "An audio drama in one act",
                description = """
                    A late-night radio host takes a call from a listener who claims to be broadcasting from tomorrow. Headphones recommended.
                """,
                author = SeedAuthors.dev, seed = "frequencies", category = SeedTaxa.audioDrama, genres = listOf(SeedTaxa.scifi, SeedTaxa.mystery), moods = listOf(SeedTaxa.nocturnal, SeedTaxa.tense),
                tags = listOf("radio", "audio drama", "time", "night"), daysAgo = 11,
                audio = true, audioMinutes = 18,
                stats = ContentStats(views = 4_300, likes = 690, comments = 1, shares = 110, ratingAverage = 4.5f, ratingCount = 98, completions = 2_200),
            )
            story(
                id = "paper-boats", accent = 0xFF2F7F8C, title = "Paper Boats on the Monsoon Drain", subtitle = "A short film about the smallest regattas",
                description = """
                    Every monsoon the children of Lane 4 race paper boats down the storm drain. This year, the adults join in.
                """,
                author = SeedAuthors.studio, seed = "paperboats", category = SeedTaxa.shortFilm, genres = listOf(SeedTaxa.memoir), moods = listOf(SeedTaxa.warm, SeedTaxa.hopeful),
                tags = listOf("monsoon", "children", "short", "city"), daysAgo = 17,
                video = SeedMedia.ESCAPES to SeedMedia.ESCAPES_THUMB, videoMinutes = 1, thumbnail = SeedMedia.thumb("paperboats"), hero = SeedMedia.hero("paperboats"),
                stats = ContentStats(views = 2_900, likes = 410, comments = 0, shares = 75, ratingAverage = 4.3f, ratingCount = 52, completions = 2_100),
            )

            comment("starlit-sea", "Leena K.", "Listened to this on a night train and had to stop at the chapter with the radio log. Beautiful.", 5, 1)
            comment("starlit-sea", "Arun", "The narration is perfect for this one. Slow in the right places.", 19, 2)
            comment("starlit-sea", "mori_reads", "Does anyone else think the voice on the frequency is Noor's mother? Re-reading chapter 2.", 41, 3)
            comment("starlit-sea", "Priya D.", "Watched the film version after reading. Different, but the same feeling.", 70, 4)
            comment("neon-harbor", "Caspian", "Ira Bose deserves a whole series.", 8, 1)
            comment("neon-harbor", "noir_fan_88", "The cold-storage reveal got me. Didn't see it coming.", 30, 2)
            comment("neon-harbor", "Leena K.", "Read this one. The prose is sharper than the audio lets on.", 55, 3)
            comment("clockwork-forest", "Hana", "Read this to my daughter over three nights. She now ticks like the owls.", 12, 1)
            comment("clockwork-forest", "wintermute", "Pell's walk to the spring is one of the calmest things I've read this year.", 90, 2)
            comment("crimson-dune", "Omar", "The cartographer is the best liar in fiction.", 100, 1)
            comment("winter-garden", "Hana", "Frost cultivated like fruit. I keep thinking about that line.", 200, 1)
            comment("winter-garden", "mori_reads", "A quiet companion to Clockwork Forest. Read them together.", 260, 2)
            comment("coffee-rain", "Sana", "Cried in a café. Appropriate.", 3, 1)
            comment("coffee-rain", "Arun", "The last chapter is twenty minutes of two people not saying the thing and it is perfect.", 14, 2)
            comment("coffee-rain", "jp_", "Short, warm, exactly right for a rainy commute.", 26, 3)
            comment("unfinished-chronicles", "Caspian", "'Some stories are better left half-told' — this essay earned that line.", 40, 1)
            comment("nodir-opare", "ঋতু", "ঘাটের দৃশ্যটা পড়ে অনেকক্ষণ চুপ করে বসে ছিলাম।", 6, 1)
            comment("nodir-opare", "Arun", "Listening in Bengali for the first time in years. Thank you for this.", 22, 2)
            comment("shesh-tram", "সৌরভ", "ট্রামের ঘণ্টার শব্দটা যেন লেখা থেকেই শোনা যাচ্ছে।", 10, 1)
            comment("night-frequencies", "wintermute", "Headphones. Lights off. Trust me.", 48, 1)
        }

        val collections = listOf(
            Collection("col-begin", "Begin here", "Four stories that show what this place is for.", listOf("starlit-sea", "clockwork-forest", "coffee-rain", "nodir-opare")),
            Collection("col-night", "Night reading", "For when the house is quiet.", listOf("neon-harbor", "shesh-tram", "night-frequencies", "winter-garden")),
            Collection("col-short", "Under twenty minutes", "Whole stories that fit a commute.", listOf("coffee-rain", "night-frequencies", "small-harbour", "paper-boats")),
            Collection("col-bengali", "বাংলায়", "Stories told in Bengali.", listOf("nodir-opare", "shesh-tram")),
        )

        return CatalogSnapshot(b.content, b.chapters, SeedTaxa.all, collections, b.comments)
    }
}
