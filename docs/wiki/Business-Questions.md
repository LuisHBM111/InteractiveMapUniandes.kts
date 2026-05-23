# Business Questions

Lista completa de BQs por sprint. Las de sprint 4 son nuevas (distintas de las anteriores), como pide la rubrica.

## Sprint 2 (vistas base)

1. Cual es la ruta del punto A al punto B?
2. Donde estoy en el campus?

## Sprint 3 (backend + Room)

1. Cuales son los restaurantes filtrados por categoria de comida y rango de precio?
2. Cual es mi proxima clase y como llego a ella?
3. Que alertas estan activas cerca de mi en este momento?

## Sprint 4 (notas + insights)

1. **A que hora del dia me muevo mas por el campus y que edificios visito mas?**
   - Cubierta por `InsightsActivity` + tabla `visits` (`VisitEntity` + `VisitDAO.visitsByHour` + `VisitDAO.topPlaces`).
   - La hora se calcula con `CAST(((enteredAt / 1000 / 3600) % 24) AS INTEGER)` en el query y se ordena por count desc.
   - Top 5 edificios viene del mismo dao en otra query, paralelizada con `async`.
2. **En que edificios escribo mas notas y cuantas tengo todavia sin sincronizar?**
   - Cubierta por `NotesActivity` + tabla `notes` (`NoteEntity` + `NoteDAO.topPlacesByNoteCount` + `NoteDAO.pendingCount`).
   - El badge "X sin sincronizar" en el header refleja `pendingCount()` en vivo.

## BQs del backend (para los TAs que pregunten)

El backend tiene endpoints `/api/v1/analytics/{usage, crash, location}` que reciben pings de la app. La idea es responder con SQL estas otras BQs (no estan UI-side aun, son backend-only):

- Crash hotspots por feature.
- Hora pico de uso por feature.
- Foot traffic en horas de almuerzo (`lunchPing`).
- CTR de ads / route satisfaction.
