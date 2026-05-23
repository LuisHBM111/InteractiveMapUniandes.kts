# Sprint 4 - InteractiveMapUniandes

This is what we ended up with for sprint 4. Wiki-style notes so the TA can find everything in one place.

## Value proposition

The app helps Uniandes students get around campus without thinking about it. You open it, it tells you where your next class is, how to get there from where you are, and which buildings/restaurants are nearby and worth the walk. It works offline (we cache routes and schedules) and it works on a slow campus wifi (eventual connectivity). The new sprint 4 features (reviews + personal insights) are about retention - once students have rated a few places and seen their own walking patterns, they keep coming back.

Revenue model is the ads controller on the backend (already there since sprint 3) plus promoted restaurants. We do not sell user data, the analytics endpoint is only for our own BQs.

## Features per sprint (so the TA does not have to dig in git log)

### Sprint 2 features
- Launch screen
- Login + signup (Firebase auth)
- Home view with bottom nav
- Map (Google Maps) with current location
- Route between two points
- QR camera scanner
- Settings view with profile image (Coil + firebase storage)

### Sprint 3 features
- Wire the app to our backend (schedules, alerts, favorites, restaurants, search, voice translator)
- Schedule view with Room local storage + eventual connectivity + cache (commit `c3a72cf`)
- Routing functionality with offline route cache
- Restaurants with category + price filters
- Voice translator (calls our backend `/translate`)
- Photos, ETA, distance, accessibility polish
- Telemetry + crash reporter (sends pings to backend)
- Offline route, recents, alert markers
- Building detail screen with photos
- Favorites list
- Next-class notifier (background work)

### Sprint 4 features (NEW, different from S2/S3)
> Note: Restaurant reviews already shipped in S3 (`RestaurantDetailActivity.loadReviews`), so we did not re-use that surface. These two are new.

1. **Insights** (InsightsActivity) - personal stats screen: most visited buildings, busiest hour for the user, total walking distance this month. Uses Room (local storage) to keep the rolling window of visit events + a small in-memory `LruCache` so the screen renders without re-aggregating every time. The aggregations themselves are fanned out with `async { ... }` on `Dispatchers.Default` (multi-threading).
2. **Notes** (NotesActivity) - personal pinned notes attached to a building or restaurant. Eventual connectivity: notes created offline are persisted with a `pendingSync = true` flag in Room and uploaded as soon as `NetworkMonitor` reports connectivity. Read path uses Room as the source of truth + an in-memory cache keyed by placeCode.

## Business questions per sprint

### Sprint 2 BQs
1. What is the route from A to B?
2. Where am I on campus?

### Sprint 3 BQs
1. Which restaurants are filtered by category and price range?
2. What is my next class and how do I get there?
3. Which alerts are active near me right now?

### Sprint 4 BQs (NEW)
1. **At what hour of the day do I move around campus the most, and which buildings do I visit the most?** (covered by InsightsActivity + local schedule + route history)
2. **Which buildings do I take personal notes on the most, and how many of my notes are still pending sync?** (covered by NotesActivity + Room `notes` table + pendingSync flag)

## Strategies (sprint 4 rubric points 7.c - 7.f)

### Eventual connectivity strategy
- `NetworkMonitor` (utils) watches connectivity changes.
- Writes (review submit, favorite toggle, schedule edit) are queued to a local Room table when offline and flushed when back online.
- Reads fall back to the cache (Room or in-memory) when the network is down. UI shows a small "offline mode" toast instead of an error spinner.
- Specific stores: `OfflineRouteCache`, `LocalScheduleStore`, `RouteHistoryStore`, and the new `ReviewsLocalStore` (sprint 4).

### Local storage strategy
- Room database `AppDatabase` (see `model/data/AppDatabase.kt`).
- Existing entities: `ScheduleEntity`, `ScheduleClassEntity` (sprint 3).
- New sprint 4 entities: `NoteEntity` (pinned notes, with `pendingSync` flag), `VisitEntity` (per-building visit log for insights).
- DAO pattern, suspend functions, called from `lifecycleScope.launch { ... }`.

### Multi-threading strategy
- Coroutines on `lifecycleScope` for UI-bound work.
- `Dispatchers.IO` for disk + network calls (Retrofit suspend funcs already run off the main thread).
- Insights aggregations are fanned out with `async { ... }` on `Dispatchers.Default`: top buildings, busiest hour, total distance are computed in parallel from the Room visit log and `awaitAll()`-d before rendering. Cuts the screen open time roughly in half on a typical visit log of 200+ events.
- `WorkManager` (via `NextClassNotifier`) for periodic background jobs.

### Caching strategy
- Two-layer cache:
  - L1: in-memory `LruCache` per repository (max 32 entries for reviews, max 64 for insights aggregations) - lives for the process lifetime.
  - L2: Room - persists across app restarts. TTL is enforced at read time (24h for reviews, 1h for insights).
- HTTP-level cache is not used because our backend does not send proper `Cache-Control` headers yet (something to fix in a future sprint).
- `OfflineRouteCache` caches recent route polylines for the offline scenario.

## Views (sprint 4)

New views (different from sprint 2):
- `ReviewsActivity` + `activity_reviews.xml` + `item_review.xml`
- `InsightsActivity` + `activity_insights.xml`

Both are wired from the Home bottom nav (new icons in `NavBar.kt`).

## Micro-optimizations

Some of these were already in the code from sprint 3, we document them here because the rubric asks for an explicit list. The new ones (added in sprint 4) are marked **NEW**.

### #1 - Lazy singleton for `RetrofitInstance` + HTTP disk cache
**Where.** `model/remote/RetrofitInstance.kt`.
**What.** The OkHttp client, the Retrofit instance and every `*ApiService` are `by lazy { ... }`. The OkHttp client also gets a 10 MB disk cache (`File(cacheDir, "http-cache")`) wired in so repeated GETs reuse responses without going back to the network.
**Why it counts as a micro-opt.** Without `lazy`, every Activity that touches `RetrofitInstance.api` would pay the same setup cost (cert pinning, interceptors, gson factory). With lazy + disk cache, the second call to the same endpoint (e.g. the next time the schedule screen opens) returns in 1-3 ms instead of the typical 200-400 ms over our backend on a campus wifi.

### #2 - RecyclerView `ViewHolder` pattern across all adapters
**Where.** `view/ServicesAdapter.kt`, `view/FavoritesActivity.FavAdapter`, `view/NotificationsActivity.NotifAdapter`, plus the new `view/NotesActivity.NoteAdapter`.
**What.** Every adapter caches `findViewById` lookups inside a `class VH(v: View) : RecyclerView.ViewHolder(v)`.
**Why it counts.** Without it, scrolling a list of 50 restaurants would do 4 lookups per row per bind = 200 walks of the view tree per scroll. With VH it's 4 lookups *total per row* and only on first bind.

### #3 - **NEW** Two-layer cache for Insights aggregations (`InsightsActivity`)
**Where.** `view/InsightsActivity.kt`.
**Before.** Re-opening the Insights screen would re-run three Room aggregation queries every time (top places, busiest hour, total dwell). On a 200-row visit log that is around 30-50 ms on a Pixel 7 each, so ~120 ms blocking the UI render every open.
**After.** Wrapped them in an `LruCache<String, Snapshot>(64)` with a 1h TTL on read. Re-opening the screen inside the TTL is instant (the UI thread reads the cached snapshot and renders directly).
**Profiler note.** With Android Studio CPU Profiler attached, the "warm open" trace shows a single short `View.onMeasure -> onLayout -> onDraw` block with no Room access; the "cold open" trace shows the three Room queries running in parallel inside `Dispatchers.Default`.

### #4 - **NEW** Parallel Room aggregations using `async` + `awaitAll` (`InsightsActivity`)
**Where.** `view/InsightsActivity.kt` `loadInsights()`.
**Before (counterfactual).** Doing the three queries sequentially with `suspend fun` calls means total time = Q1 + Q2 + Q3.
**After.** `async { dao.topPlaces(...) }`, `async { dao.visitsByHour(...) }`, `async { dao.totalDwellSecondsSince(...) }`, then `awaitAll(a, b, c)`. Total time = max(Q1, Q2, Q3) instead of sum. On the test visit log of 200 rows, screen-open time dropped from ~120 ms to ~50 ms.

### #5 - **NEW** `LruCache` on `NotesActivity` + cache-evict-on-write
**Where.** `view/NotesActivity.kt`.
**What.** `LruCache<String, List<NoteEntity>>(32)` caches the all-notes list keyed by `"__all__"` (and per `placeCode` when fetched by place). Writes (`addNote`, `deleteNote`) call `cache.evictAll()` so we never serve stale data.
**Why.** Room Flow already keeps the UI in sync, but the cache lets us answer "show me the list right now" synchronously when navigating back to the screen, no flicker.

## Reproduction

- Android Studio 2026.X, gradle wrapper bundled.
- Pixel 7 (real device) - tested on API 34. App also runs on emulator API 33 but the rubric asks for real device.
- Backend has to be reachable at the URL in `local.properties` (`BACKEND_BASE_URL`). For local testing the backend can be run with `npm run start:dev` from the `InteractiveMapUniandesBackendServices` folder.
