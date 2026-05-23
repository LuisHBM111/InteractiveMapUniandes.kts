# InteractiveMapUniandes - Wiki

App movil para que un estudiante de Uniandes sepa donde es su proxima clase, como llegar y que hay cerca. Trabaja offline (rutas y horario cacheado) y aguanta el wifi del campus (eventual connectivity).

Esta wiki cubre los 4 sprints del curso ISIS-3510. Cada sprint tiene su pagina separada con features, business questions y estrategias.

## Indice

- [Sprint 2](Sprint-2) - vistas base, login, mapa, ruta, QR, settings
- [Sprint 3](Sprint-3) - integracion al backend, schedule con Room, restaurantes con filtros, traductor por voz, telemetria
- [Sprint 4](Sprint-4) - notas personales, insights, micro-optimizaciones, threading, caching
- [Strategies](Strategies) - eventual connectivity, local storage, multi-threading, caching (las 4 que pide la rubrica de sprint 4)
- [Business Questions](Business-Questions) - lista completa por sprint
- [Micro-optimizations](Micro-optimizations) - antes / despues con evidencia

## Value proposition

El estudiante abre la app y la app le resuelve "donde estoy, donde va mi proxima clase, como llego, que como cerca". Sin pensar. Las features de sprint 4 (notas + insights) son para retencion: cuando el estudiante ya califico restaurantes y ya tiene notas pegadas a edificios, vuelve a la app porque sus datos estan ahi.

Revenue model: ads controller del backend (ya esta desde sprint 3) + restaurantes promocionados. No vendemos data de usuarios, el endpoint de analytics es solo para nuestras BQs.

## Equipo y repos

- Android: [LuisHBM111/InteractiveMapUniandes.kts](https://github.com/LuisHBM111/InteractiveMapUniandes.kts)
- Backend: [LuisHBM111/InteractiveMapUniandesBackendServices](https://github.com/LuisHBM111/InteractiveMapUniandesBackendServices)

## Como correrlo

- Android Studio 2026.X, gradle wrapper bundled, JDK 21.
- Real device recomendado (Pixel 7, API 34). Funciona en emulador API 33 pero la rubrica pide real device.
- Configurar `local.properties` con `BACKEND_BASE_URL=https://...` apuntando al backend desplegado, o levantar backend local con `npm run start:dev` desde `InteractiveMapUniandesBackendServices/`.
- Para los TAs: invitacion via Firebase App Distribution con el patron `isis3510-<team>-android-Sprint4`.
