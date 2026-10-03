# Olot Palot · ওলট পালট

A multimedia story platform for Android. Every story can be **listened to**, **read**, and — where a film exists — **watched**, inside one editorial, magazine-like experience.

Native Android · Kotlin · Jetpack Compose · Room · DataStore · Media3 · Hilt.

## Status

**Milestone 1 (this build):** design system, typographic navigation, Home, Discover + search/filter, Story detail, Listen, Read Story, Watch, Library, Profile, four persistent themes (+ image-derived custom theme), likes / favourites / save-for-later / ratings / comments / bookmarks / share, reading + listening + watching progress with a persistent *Continue* strip, rewarded-access architecture with a mock ad provider, local analytics, deep links.

Data comes from a bundled seed catalog (11 stories, English and Bengali, with original chapter text). The Supabase backend plugs in behind the same repository interfaces — see *Backend*.

## Build

Requirements: JDK 17+ (21 works), Android SDK with platform 35 and build-tools 35.0.0.

```bash
./gradlew :app:assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest :domain:test
```

Install on a phone with developer mode: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions builds the debug APK on every push to `main`; download it from the workflow run's artifacts.

## Architecture

```
app                 Activity, Hilt app, navigation graph, app shell (nav rail + continue strip)
core/common         AppResult, dispatchers, Clock, time formatting, analytics abstraction
core/design         Design system: palettes, typography (Fraunces / Manrope / Noto Serif Bengali), tokens, components
core/navigation     Type-safe routes shared by all features
core/database       Room entities + DAOs
core/datastore      Preferences (theme, reader text size, speed, reduced motion, display name) + local user identity
domain              Pure Kotlin: models, repository interfaces, ranking engine, search evaluator, use cases
data                Repository implementations, seed catalog, remote boundary, access/reward, analytics
feature/*           home · discover · story (detail + comments) · reader · player (listen + watch) · watch · library · profile
```

Compose UI → ViewModel → use case → repository interface → Room (observed) / remote data source (refresh). Composables never touch Room.

### State separation

| State | Where it lives |
|---|---|
| UI state | ViewModel `StateFlow`s |
| Session state | `MainViewModel` (dismissed continue strip), player instances |
| Local persistent | DataStore (preferences), Room (progress, bookmarks, relationships, comments) |
| Cached content | Room `content`, `media_assets`, `chapters`, `taxa`, `collections` |
| Remote | `RemoteContentDataSource` (seed now, Supabase later) |

Theme state and navigation state never read each other: `OlotPalotTheme` is driven only by DataStore, the nav rail only by the back stack.

### Backend

`data/remote/RemoteContentDataSource` is the single boundary. `SeedRemoteContentDataSource` is bound in `DataBindsModule`; replacing that one binding with a Supabase implementation switches the app to the real backend. Engagement writes are stored locally with per-relationship timestamps and a `dirty` flag so a sync worker can replay them. No privileged key ever ships in the APK.

## Tests

- `domain`: search filtering/sorting, ranking engine
- `core/datastore`: theme persistence across simulated process death (Standard / Light / Dark / Sunrise), recent-search dedupe

## Seed media

Artwork uses `picsum.photos`; video uses Blender Foundation films from Google's sample bucket; audio uses ExoPlayer's test tracks. These are placeholders: metadata durations describe the fictional stories, not the sample files.
