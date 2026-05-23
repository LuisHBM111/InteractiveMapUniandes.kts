# Strategies

Esta pagina cubre las 4 estrategias que la rubrica de sprint 4 pide listar (puntos 7.c, 7.d, 7.e, 7.f).

## Eventual connectivity

- Helper: `utils/NetworkMonitor.kt` -> `NetworkMonitor.isOnline(context)` revisa `ConnectivityManager` con `NET_CAPABILITY_INTERNET` + `NET_CAPABILITY_VALIDATED`.
- Writes (review submit, favorite toggle, schedule edit, **nueva nota**) se guardan en local primero. Si no hay red, quedan con un flag de "pendiente" y se reintenta cuando la red vuelve.
  - Nota sprint 4 -> `NoteEntity.pendingSync = true`, flush via `NotesActivity.trySyncPending()`.
  - Schedule sprint 3 -> `LocalScheduleStore` + `ScheduleRepository`.
  - Route sprint 3 -> `OfflineRouteCache` guarda la ultima polyline para repintarla offline.
- Reads caen al cache si la red falla. La UI muestra un toast "Sin internet" en vez de un error rojo.

## Local storage

- Room database `interactivemapuniandes.model.data.AppDatabase`, file `interactive_map_uniandes.db`, version `2` con `fallbackToDestructiveMigration(dropAllTables = true)` (las tablas son solo cache, no hay user-authored content que perder).
- Entidades:
  - Sprint 3: `ScheduleEntity` (tabla `schedules`), `ScheduleClassEntity` (tabla `schedule_classes`).
  - **Sprint 4: `NoteEntity` (tabla `notes`), `VisitEntity` (tabla `visits`).**
- DAOs:
  - Sprint 3: `ScheduleDAO`.
  - **Sprint 4: `NoteDAO`, `VisitDAO`.**
- DataStore tambien aparece en `PreferencesRepository` (sprint 3) para flags simples del usuario.
- Files en disco: `OfflineRouteCache` para polylines, `RouteHistoryStore` para recientes.

## Multi-threading / concurrency

- Coroutines: `lifecycleScope.launch { ... }` en cada Activity para trabajo UI-bound.
- Dispatchers:
  - `Dispatchers.IO` para disco + Retrofit (las suspend funcs de Retrofit lo manejan solo).
  - **`Dispatchers.Default` para las agregaciones de Insights** (CPU-bound puro).
- Patron de fan-out en sprint 4 (`InsightsActivity.loadInsights()`):

      val a = async { dao.topPlaces(since, 5) }
      val b = async { dao.visitsByHour(since) }
      val c = async { dao.totalDwellSecondsSince(since) }
      val results = awaitAll(a, b, c)

  Tiempo total = max(Q1, Q2, Q3) en vez de Q1+Q2+Q3.
- WorkManager (sprint 3): `NextClassNotifier` para alarmas previas a la siguiente clase.
- Telemetria (sprint 3): `CoroutineScope(SupervisorJob() + Dispatchers.IO)` propia que sobrevive lifecycle, para que un crash al postear no tumbe el job.

## Caching

Dos capas:
- L1 in-memory `LruCache`:
  - `NotesActivity` -> `LruCache<String, List<NoteEntity>>(32)` keyed por `placeCode` (o `"__all__"`).
  - `InsightsActivity` -> `LruCache<String, Snapshot>(64)` con TTL de 1 h.
- L2 Room (persiste al cerrar la app, TTL via columna `cachedAt` o re-fetch en background).
- HTTP-level: `RetrofitInstance` mete un `okhttp3.Cache(File(cacheDir, "http-cache"), 10 MB)` para GETs que el backend marque cacheables (el backend no esta mandando `Cache-Control` todavia, esto queda como backlog).
- Image cache: Coil 3 maneja su propia disk + memory cache (sprint 3).

## Resumen de donde vive cada estrategia

| Estrategia | Sprint 3 | Sprint 4 |
|---|---|---|
| Eventual connectivity | NetworkMonitor, OfflineRouteCache, LocalScheduleStore | NoteEntity.pendingSync, NotesActivity.trySyncPending |
| Local storage | AppDatabase v1 (schedules) | AppDatabase v2 (notes, visits) |
| Multi-threading | lifecycleScope + Dispatchers.IO, WorkManager | async + awaitAll en Dispatchers.Default |
| Caching | OkHttp 10 MB disk cache, Coil disk+mem | LruCache + TTL en Notes/Insights |
