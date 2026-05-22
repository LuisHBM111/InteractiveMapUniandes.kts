# Sprint 3 - Backend, Room, restaurantes, traductor

En sprint 3 conectamos la app al backend propio (NestJS), metimos Room para schedule, agregamos restaurantes con filtros, traductor por voz y telemetria.

## Features

- Wire al backend: schedules, alerts, favorites, restaurants, search, voice translator (commit `30d0c93`)
- Schedule view con Room + eventual connectivity + multi-threading + caching (commit `c3a72cf` - literalmente trae las 4 estrategias)
- Routing functionality con offline route cache (commit `40b112c`)
- Restaurantes con filtros de categoria y precio (commit `5963752`)
- Voice translator (llama a nuestro endpoint `/translate` que envuelve MyMemory)
- Photos, ETA, distance, accessibility polish (commit `1ba07a2`)
- Telemetria + crash reporter (manda pings al backend para BQs)
- Offline route, recents, alert markers, building detail, favoritos, next-class notifier (commit `0182036`)
- QR scanner ya conectaba al backend para resolver el edificio escaneado
- Sensor de ubicacion (FusedLocationProvider) para "donde estoy" + route from current

## BQs de sprint 3

1. Cuales son los restaurantes filtrados por categoria de comida y rango de precio?
2. Cual es mi proxima clase y como llego a ella?
3. Que alertas estan activas cerca de mi en este momento?

## Vistas nuevas en sprint 3 (diferentes de sprint 2)

- `ScheduleActivity` + `SchedulesActivity`
- `RestaurantsActivity` + `RestaurantDetailActivity` (incluye reviews y rating)
- `VoiceTranslatorActivity`
- `AlertsActivity` + `NotificationsActivity`
- `FavoritesActivity`
- `BuildingDetailActivity`
- `OfflineMapsActivity`

## Backend (servicios montados en sprint 3)

- `/api/v1/restaurants` con filtros `?foodCategory=...&price=...`
- `/api/v1/places` + `/api/v1/places/buildings`
- `/api/v1/routes/graph/nearest` para rutear desde la posicion actual
- `/api/v1/schedules` (con parser ICS)
- `/api/v1/alerts` y `/api/v1/notifications`
- `/api/v1/favorites`
- `/api/v1/analytics/{usage, crash, location}` (Telemetria)
- `/api/v1/ads` (controller para revenue model)
- `/api/v1/translate` (envuelve MyMemory con retry)

## Estrategias en sprint 3

- Eventual connectivity: ya con NetworkMonitor + `OfflineRouteCache` + `LocalScheduleStore`
- Local storage: Room (`AppDatabase` con `ScheduleEntity` y `ScheduleClassEntity`)
- Multi-threading: coroutines en `lifecycleScope` + `Dispatchers.IO` (Retrofit suspend funcs)
- Caching: HTTP disk cache de 10 MB en `RetrofitInstance`, mas la cache de Room
