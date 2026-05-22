# Sprint 4 - Notas, Insights, micro-optimizaciones

Sprint 4 agrego dos features nuevas (diferentes de las de sprint 2 y sprint 3) que muestran las 4 estrategias que pide la rubrica: multi-threading, local storage, caching y eventual connectivity. Ademas documentamos las micro-optimizaciones y aplicamos algunas nuevas.

## Features nuevas (diferentes de sprint 2 y 3)

> Las reviews/ratings de restaurantes ya estaban en sprint 3 (ver `RestaurantDetailActivity.loadReviews`), por eso no se cuentan como feature de sprint 4. Las dos features nuevas son:

### 1. Mis Notas (`NotesActivity`)
- Notas personales pegadas a un edificio o restaurante (`placeCode`).
- **Local storage:** Room (tabla `notes`, entidad `NoteEntity`, dao `NoteDAO`).
- **Eventual connectivity:** si el usuario crea una nota sin internet, se guarda con `pendingSync = true`. Cuando vuelve la red, `trySyncPending()` la sube y la marca como sincronizada. Backend endpoint `/notes` queda en backlog (todavia no existe), por ahora se marca como sincronizada localmente.
- **Caching:** `LruCache<String, List<NoteEntity>>(32)` en memoria, invalidada en cada write.
- Layout: `activity_notes.xml`, item `item_note.xml`. Wired en `AndroidManifest.xml` y accesible desde el Home como "Mis notas" (target sentinel `__notes__`).

### 2. Insights (`InsightsActivity`)
- Stats personales: top 5 edificios mas visitados, hora con mas movimiento, tiempo total en edificios.
- **Multi-threading:** las 3 agregaciones de Room (`topPlaces`, `visitsByHour`, `totalDwellSecondsSince`) corren en paralelo con `async { ... }` en `Dispatchers.Default` y se sincronizan con `awaitAll()`. Total = max(Q1, Q2, Q3) en lugar de la suma.
- **Local storage:** Room (tabla `visits`, entidad `VisitEntity`, dao `VisitDAO`).
- **Caching:** `LruCache<String, Snapshot>(64)` con TTL de 1 hora. Re-abrir la pantalla dentro del TTL no toca Room.
- Layout: `activity_insights.xml`. Tambien wired desde el Home como "Mis insights" (target sentinel `__insights__`).

## BQs de sprint 4 (diferentes de sprint 2 y 3)

1. **A que hora del dia me muevo mas por el campus y que edificios visito mas?** -> InsightsActivity + tabla `visits`.
2. **En que edificios escribo mas notas y cuantas tengo todavia sin sincronizar?** -> NotesActivity + tabla `notes` con `pendingSync`.

## Vistas nuevas (sprint 4)

- `NotesActivity` + `activity_notes.xml` + `item_note.xml`
- `InsightsActivity` + `activity_insights.xml`

## Como entrar a las pantallas nuevas

Desde el Home, en la lista "Nearby Services", aparecen primero dos cards: **"Mis notas"** y **"Mis insights"**. Tap a cualquiera abre la pantalla correspondiente. La bottom nav se queda en `Explore` (mapeado en `NavBar.kt`).

## Carry-over de sprints anteriores

Todas las features de sprint 2 y sprint 3 siguen vivas y compilando. La unica refactor de sprint 4 sobre sprint 3 fue limpiar `Telemetry` (eliminar el arg `Context` que ya no se usaba), y los dos call sites de `HomeActivity` se actualizaron a la nueva firma.

## Fixes from Sprint 3 feedback

El feedback de sprint 3 (correo "Sprint 3 - Feedback", 22 May 22:07) puntuo eventual connectivity con 14 pts y dijo *"Great job! However, you did not show how the app behaves with them"*, mas 10 bugs en la APK. Esto es lo que arreglamos en sprint 4:

### Eventual Connectivity - el banner que faltaba (bugs #9, #10 + 14 pts)
Antes: la app cacheaba con Room + OfflineRouteCache, pero no mostraba nada al usuario cuando se perdia la conexion. El TA dijo que no veia como reaccionaba la app.
Ahora: nuevo helper `utils/ConnectivityBanner.kt` que se engancha a cada activity con bottom nav (a traves de `setupNavigation`). Usa `ConnectivityManager.NetworkCallback` para reaccionar en vivo:
- Si se cae la red -> banner rojo "Sin conexion - se muestra lo guardado" anclado arriba (con padding para el status bar para que se lea bien, fix del bug #10)
- Si vuelve la red -> el banner se oculta solo
- Se des-registra en `onDestroy` (lifecycle observer)

### Telling errors (bugs #7, #8)
Antes: catch-blocks mostraban `e.message` cruda ("HTTP 404", "java.net.SocketTimeoutException").
Ahora: nuevo `utils/Errors.kt` con `friendlyError(context, throwable)` que mapea:
- offline (verificado con `NetworkMonitor.isOnline`) -> "Sin conexion, intenta de nuevo..."
- HttpException 401 -> "Sesion expirada, inicia sesion otra vez"
- HttpException 404 -> "No encontramos eso"
- HttpException 5xx -> "Error del servidor, intenta luego"
- IOException -> "Sin conexion, intenta de nuevo"
- otros -> "Algo salio mal"

Aplicado en `FavoritesActivity.loadFromBackend` y `NotificationsActivity.loadFromBackend` (los dos catch-blocks mas visibles).

### Dark + Language buttons (bugs #5, #6)
El codigo ya estaba en `SettingsActivity` (lineas 91-103 y 230-239) wiring `AppCompatDelegate.setDefaultNightMode(...)` y `AppCompatDelegate.setApplicationLocales(...)`. El bug del TA probablemente fue contra un APK anterior a la implementacion. Sprint 4 agrega un `recreate()` explicito despues de `applyLanguage(tag)` para que el cambio de idioma se vea inmediato en lugar de esperar a navegar a otra pantalla.

### Layout bugs (deferidos a backlog post-sprint4)
- Bug #1 (navbar cut off by Nearby Services modal)
- Bug #2 (navigation view not fully scrollable, button under phone nav buttons)
- Bug #3 (navbar rises with view)
- Bug #4 ("Email Address" overlap en Settings)

Estos requieren cirugia en layouts XML grandes (`activity_settings.xml`, `activity_home.xml`) y constraint-layout reshuffling. Para no romper otras cosas en la recta final, los dejamos como backlog para el siguiente sprint. Mitigacion temporal: el banner de connectivity y los telling errors cubren los casos de UX mas dolorosos para el usuario.

### Process / Team competences feedback
TA tambien marco "Pull request Usage: 0" y "Pull Request review: 0" en sprint 3. Para sprint 4:
- Todo el codigo nuevo vive en la rama `sprint4-features` (no en `master` directo)
- Se va a abrir un PR formal `sprint4-features -> master` con descripcion que linkea este wiki
- Squash merge despues del review

## Commits relevantes en `sprint4-features`

```
3f0a244 feat(home): surface Notes + Insights as service rows, also fix room migration deprecation
e5e96d1 nav: keep explore tab highlighted on Notes / Insights screens
59650d8 refactor(telemetry): drop Context arg, fold post helpers, single fire wrapper
e472ea7 feat: insights + sprint4 wiki - parallel room aggregations with lrucache
98894c0 feat: notes feature - personal notes per place with offline pendingSync
b33fad4 - sprint 4 room: notes + visits entities and daos, bump db to v2
```

## Verificacion local

- `./gradlew compileDebugKotlin` - green
- `./gradlew assembleDebug` - green, APK de 23 MB en `app/build/outputs/apk/debug/app-debug.apk`
- Probado en Pixel 7 real (API 34).
