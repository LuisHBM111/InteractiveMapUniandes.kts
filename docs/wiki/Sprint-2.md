# Sprint 2 - Vistas base

Features que entregamos en sprint 2 (todas siguen funcionando en sprint 4).

## Features

- Launch screen
- Login + signup con Firebase Auth
- Home view con bottom nav
- Google Maps con ubicacion actual del usuario
- Ruta entre dos puntos (sin backend todavia, era hardcoded)
- Camara QR para escanear codigos de edificios
- Settings view con foto de perfil (Coil + Firebase Storage)

## BQs de sprint 2

1. Cual es la ruta del punto A al punto B?
2. Donde estoy en el campus?

## Vistas implementadas en sprint 2

- `LaunchActivity` / `LoginActivity` / `SignupActivity`
- `HomeActivity` con `SupportMapFragment`
- `SearchActivity` (busqueda basica)
- `RouteActivity` (primera version)
- `SettingsActivity`

## Notas

- Material 3 instalado en este sprint.
- `NavBar.kt` aparece aqui pero el routing entre tabs era manual via Intents.
- No habia Room todavia, ni backend propio. Las rutas eran hardcoded en assets.
