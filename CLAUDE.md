# Code style for this project

This file is the source of truth for how Claude (or any AI) writes code in
this repo. **Match it.** Don't drift toward "professional" formatting.

## Comments

- **Inline only**, at the end of a line, after `//` or `# `.
- Short, casual, capitalised first letter, sometimes Spanish ("// Notify user", "// Dato corto para la card derecha").
- Describe **what** the line is doing, not why or how. Skip if obvious.
- Never write KDoc / multi-line `/** ... */` blocks unless the function is exported across modules.
- Never write `// TODO`, `// FIXME`, `// XXX`. Just do it or drop it.

```kotlin
// GOOD
val source = BuildConfig.TRANSLATE_SOURCE_LANG.substringBefore('-') // "es-ES" -> "es"

// BAD (over-explains, multi-line)
/**
 * Extracts the primary language subtag from the BCP-47 source language
 * configured at build time, e.g. "es-ES" becomes "es".
 */
val source = BuildConfig.TRANSLATE_SOURCE_LANG.substringBefore('-')
```

## Code shape

- Activities can be 200-400 lines, that's fine. Don't over-extract.
- Prefer one private function per UI section, called from `onCreate`.
- No abstract base classes, no DI framework, no sealed-class gymnastics unless the file already uses them.
- `findViewById` is fine. ViewBinding optional.
- `lifecycleScope.launch { ... }` for coroutines. No custom dispatchers.

## Error handling

- Network call → try/catch → `Toast.makeText(...)` with the error message.
- Don't introduce `Result<T>` / sealed `UiState` if the screen doesn't already use them.
- 401 / 503 → tell the user "sign in again" or "try later". Don't redirect anywhere automatically.

## Naming

- camelCase for vars and functions, PascalCase for classes.
- Mirror the variable names already used in nearby code, even if they're not perfect (`btnImport` not `importButton`).

## Strings

- Inline string literals are fine for one-off Toasts and labels nobody else needs.
- For anything user-visible that lives in a layout, use `strings.xml` so i18n works.
- Spanish as primary, English as `values-en/`.

## Style of explanations to the user

- Lowercase, casual, like a teammate Slack message.
- Short. No marketing-style bullet lists with bold headers unless explicitly summarising work.
- If something doesn't work, say "X is broken because Y, fixing".
