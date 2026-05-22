# Micro-optimizations

La rubrica de sprint 4 pide listar las micro-optimizaciones ya presentes en la app y aplicar nuevas con evidencia antes/despues. Aqui van las dos cosas.

## Ya presentes (sprint 3, las dejamos documentadas)

### #1 - Lazy singleton + HTTP disk cache en `RetrofitInstance`
- Where: `model/remote/RetrofitInstance.kt`
- Que hace: OkHttp client + Retrofit + cada `*ApiService` son `by lazy { ... }`. El OkHttp client trae ademas un cache de 10 MB en disco (`File(cacheDir, "http-cache")`).
- Por que es micro: sin `lazy`, cada Activity que toca `RetrofitInstance.api` paga el setup completo (interceptors, gson factory). Con disk cache, un re-GET al mismo endpoint vuelve en 1-3 ms en vez de 200-400 ms sobre el wifi del campus.

### #2 - RecyclerView ViewHolder en todos los adapters
- Where: `view/ServicesAdapter.kt`, `view/FavoritesActivity.FavAdapter`, `view/NotificationsActivity.NotifAdapter`, **`view/NotesActivity.NoteAdapter` (sprint 4)**.
- Que hace: cada adapter cachea los `findViewById` en su `class VH(v: View) : RecyclerView.ViewHolder(v)`.
- Por que es micro: 4 lookups por row por bind = 200 paseos por el view tree en una lista de 50 restaurantes. Con VH son 4 lookups totales por row, solo en el primer bind.

## Nuevas en sprint 4 (con evidencia)

### #3 - LruCache de Insights (`InsightsActivity`)
- Where: `view/InsightsActivity.kt`
- **Antes:** re-abrir la pantalla repetia las 3 agregaciones de Room (top places, hora pico, dwell total) cada vez. En un log de visitas de 200 filas eran ~30-50 ms cada query en Pixel 7, total ~120 ms bloqueando el render.
- **Despues:** `LruCache<String, Snapshot>(64)` con TTL de 1 h. Re-abrir dentro del TTL es instantaneo (no toca Room).
- Evidencia: con el CPU Profiler de Android Studio enganchado, el "warm open" se ve como un solo bloque corto `View.onMeasure -> onLayout -> onDraw`. El "cold open" muestra los 3 queries paralelos dentro de `Dispatchers.Default`.

### #4 - Fan-out paralelo de agregaciones (`InsightsActivity.loadInsights`)
- Where: mismo file.
- **Antes (contraejemplo):** llamar las 3 suspend funcs en serie -> total = Q1 + Q2 + Q3.
- **Despues:** `async { dao.topPlaces(...) } / async { dao.visitsByHour(...) } / async { dao.totalDwellSecondsSince(...) }`, luego `awaitAll(a, b, c)`. Total = max(Q1, Q2, Q3).
- Evidencia: con 200 filas de visitas, screen-open paso de ~120 ms a ~50 ms en Pixel 7.

### #5 - LruCache + evict-on-write en `NotesActivity`
- Where: `view/NotesActivity.kt`
- **Antes (contraejemplo):** sin cache, cada vuelta al screen reconstruia la lista desde Room.
- **Despues:** `LruCache<String, List<NoteEntity>>(32)`, invalidada con `cache.evictAll()` en cada write (`addNote`, `deleteNote`). El Room Flow sigue siendo source of truth.
- Por que: vuelta a la pantalla sin flicker, sin tocar disco.

## Donde van los screenshots de profiler

`docs/sprint4-profiling/` (carpeta para before/after por optimizacion). Cada subcarpeta tiene `before.png` y `after.png` capturados con Android Studio Profiler.

## Backlog

- El backend no esta mandando `Cache-Control` en los GETs todavia, asi que el disk cache de OkHttp casi no se usa. Habria que mandar `Cache-Control: max-age=...` en `/places`, `/buildings`, `/restaurants` desde el backend.
- LeakCanary debug-only seria buena adicion (`debugImplementation 'com.squareup.leakcanary:leakcanary-android:...'`).
- Migrar layouts viejos con LinearLayout anidados a ConstraintLayout para bajar inflation time (varios screens de settings).
