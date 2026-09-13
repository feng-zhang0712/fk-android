# I18n (`com.fk.core.i18n`)

Runtime locale switch, typed keys, dictionary / resources lookup, and locale formatters — beyond static `res/values`. Phase **B2**.

## Layout

| Type | Role |
|------|------|
| `I18n` | Package marker + `dictionaryManager` factory |
| `I18nConfig` / `I18nLanguage` / `I18nKey` | Config, selectable language, typed keys |
| `I18nManager` | Language switch, hybrid lookup, observers, formatters |
| `I18nDictionary` / `MapDictionary` | In-memory / remote-copy backends (`MapDictionary.ofFlat`) |
| `AndroidResourcesDictionary` | Optional `res/values` lookup under in-app locale |
| `LanguageStore` / `SharedPrefsLanguageStore` | Persist in-app selection |
| `LocaleMatcher` | BCP-47 canonicalize + fallback chains |
| `MessageFormat` | `{token}` interpolate + simple `one\|other` plurals |
| `LocaleFormatters` | Number / date formatters bound to the active locale |

## Usage

```kotlin
val manager = I18n.dictionaryManager(
  flatDictionary = mapOf(
    "en" to mapOf("greeting" to "Hello, {name}"),
    "zh-Hans" to mapOf("greeting" to "你好，{name}"),
  ),
  context = applicationContext, // optional persistence
  // attachAndroidResources = true, // dictionary miss → string resources
)

manager.setLanguageCode("zh-Hans")
val text = manager.localized("greeting", mapOf("name" to "FK"))

manager.observeLanguageChange { language ->
  // refresh UI copy
}

manager.resetLanguageSelection() // clear persistence → defaultLanguageCode
```

## Lookup order

1. Dictionary (active language, then dictionary fallback language)
2. Resources (progressive [LocaleMatcher] candidates + config fallbacks), when attached
3. Return the key unchanged

## Notes

- In-app language is independent of the system locale; first launch prefers system locales within the supported set.
- Resource names cannot contain `.`; `demo.title` is resolved as `demo_title`.
- Prefer Android / ICU plurals in `res` for production; `MessageFormat.pluralSimple` is for dictionary templates.
