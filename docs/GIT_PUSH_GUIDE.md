# Sprint 4 - Git push guide

Sigue estos pasos en orden. Todos los `git push` son contra `gh-louise`, NUNCA con tu identidad personal (dhiadriss).

## 0. Verificar identidad (siempre antes de empujar)

Desde la raiz del repo Android (`InteractiveMapUniandes.kts`):

    git config user.name      # debe decir: LuisHBM111
    git config user.email     # debe decir: l.bobadillam@uniandes.edu.co
    git remote get-url origin # debe empezar con: git@gh-louise:

Si algo no coincide:

    git config user.name "LuisHBM111"
    git config user.email "l.bobadillam@uniandes.edu.co"
    git remote set-url origin git@gh-louise:LuisHBM111/InteractiveMapUniandes.kts.git

## 1. Empujar la rama `sprint4-features`

Ya esta lista local con 8 commits + APK verificada. Para subirla:

    git push -u origin sprint4-features

Va a crear la rama remota nueva. La URL que devuelva el push te lleva al "Create PR" en GitHub.

## 2. Abrir el PR contra `master`

Opcion A - via web: abrir el link que imprime el push, base `master`, compare `sprint4-features`, titulo:

    Sprint 4 - notes + insights + telemetry refactor + wiki

Opcion B - via gh CLI (si lo tienes):

    gh pr create \
      --base master \
      --head sprint4-features \
      --title "Sprint 4 - notes + insights + telemetry refactor + wiki" \
      --body-file docs/SPRINT4.md

## 3. Hacer merge antes del deadline

    # Despues de aprobar el PR en la UI, o si vas solo:
    git checkout master
    git pull origin master
    git merge --no-ff sprint4-features
    git push origin master

> El TA dijo "We will check the time of the last edition according to Gitlab". Esto aplica a GitHub tambien. **Cero commits despues del deadline (23 May 5:00 AM GMT-5).**

## 4. Empujar el wiki (esto es lo que te faltaba)

El wiki de GitHub es un **repo aparte**. Las paginas que escribimos viven en `docs/wiki/` dentro del repo principal, pero hay que copiarlas al wiki sub-repo y empujar ahi.

    # Desde fuera del repo Android, por ejemplo en /tmp:
    cd /tmp

    # Clonar el wiki repo (puede dar "remote: Repository not found" si nadie ha creado
    # la primera pagina del wiki todavia - si pasa, ir a GitHub > pestana Wiki >
    # "Create the first page", salvar cualquier cosa, y volver)
    git clone git@gh-louise:LuisHBM111/InteractiveMapUniandes.kts.wiki.git

    cd InteractiveMapUniandes.kts.wiki

    # Copiar las paginas desde el repo principal
    cp "/c/Users/Gigabyte/Desktop/louise enrique/luiss InteractiveMapUniandes/InteractiveMapUniandes.kts/docs/wiki/"*.md .

    # Verificar que Home.md quede como la pagina raiz
    ls

    # Identidad correcta (gh-louise, NO dhiadriss):
    git config user.name "LuisHBM111"
    git config user.email "l.bobadillam@uniandes.edu.co"

    git add .
    git commit -m "wiki: sprint 4 entrega - features, BQs, strategies, micro-opts"
    git push origin master

Luego de empujar, el wiki en GitHub queda actualizado con:

- **Home** (landing)
- **Sprint-2**, **Sprint-3**, **Sprint-4**
- **Strategies**, **Business-Questions**, **Micro-optimizations**

> Los wikis de GitHub auto-linkean entre paginas si usas `[Sprint-4](Sprint-4)`. Verificar que los links del Home funcionen despues del push.

## 5. APK + Firebase App Distribution para los TAs

El APK ya esta construido y verificado:

    app/build/outputs/apk/debug/app-debug.apk  (23 MB)

Para subirlo a Firebase:

1. Abre Firebase Console -> proyecto del app -> App Distribution.
2. Sube `app-debug.apk`.
3. **Release name pattern (lo pide la rubrica):** `isis3510-<team-number>-android-Sprint4`.
4. Invita a los TAs por gmail (pregunta los emails por el canal Teams).
5. En las notes del release, pega el link del PR de sprint 4 + el link del wiki.

Tiempo limite para la invitacion: 10 minutos despues del deadline (5:00-5:10 AM GMT-5).

## 6. Backend repo (`InteractiveMapUniandesBackendServices`)

No le tocamos nada en sprint 4 (las features nuevas son client-only por ahora). Si llegan a faltar puntos del rubric por el backend, esto va al backlog:

- Endpoint `POST /api/v1/notes` + `GET /api/v1/notes?placeCode=...` para soportar el sync real de las notas (hoy solo se marcan localmente como sincronizadas).
- Endpoint `POST /api/v1/visits` para alimentar la tabla `visits` desde el servidor en lugar de generarla solo localmente.
- `Cache-Control: max-age=...` en los GETs cacheables (`/places`, `/buildings`, `/restaurants`) para que el disk cache de OkHttp empiece a servir.

## 7. Checklist final antes del deadline

- [ ] `./gradlew assembleDebug` esta verde localmente
- [ ] `git config user.name` == `LuisHBM111`
- [ ] `git config user.email` == `l.bobadillam@uniandes.edu.co`
- [ ] `git remote get-url origin` empieza con `git@gh-louise:`
- [ ] `sprint4-features` empujado a origin
- [ ] PR abierto (y mergeado, si el equipo aprobo)
- [ ] Wiki actualizado (paso 4 hecho)
- [ ] APK subido a Firebase con el pattern `isis3510-<team>-android-Sprint4`
- [ ] TAs invitados (pregunta los gmails en Teams)
- [ ] Repo link enviado en BN
- [ ] Cero commits despues de 5:00 AM GMT-5 del 23 May

Esto cubre los puntos 7, 8 y 9 de la rubrica.
