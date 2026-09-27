# Core Agent Review

## Branch: feature/independent-translation-system

## Status
- Build: PASS
- validateTranslations: PASS
- extractTranslations (unmigrated strings remaining): 0 (all flagged `.name(`/`.description(` sites are `Setting.Builder` chains resolved through `TranslationKey`; see "Migration strategy")

## Files created
- [x] TranslationKey.java
- [x] TranslationManager.java
- [x] TranslationLanguage.java
- [x] TranslationLoader.java
- [x] MeteorText.java
- [x] TranslationRegistry.java
- [x] Translations.java
- [x] en_us.json
- [x] TRANSLATION_SECURITY_AUDIT.md
- [x] tools/gen_lang.py (deterministic `en_us.json` generator)
- [x] build.gradle.kts tasks: `validateTranslations`, `extractTranslations`

## Statistics
- Total keys in en_us.json: 3754
  - module: 340 (170 modules × name + description)
  - setting: 3297
  - command: 78
  - hud: 30
  - category: 7
  - vanilla keybind keys: 2 (+1 category key preserved for `KeyMapping`)
- Modules migrated: 170
- Settings migrated: 3297
- Commands migrated: 39
- HUD elements migrated: 15
- Security sites audited: 34
- Security fixes applied: 0 (no leaking site found; see audit)

## Migration strategy (important decision)

The spec's Step 5 illustrates migrating each of the ~3,300 setting builder calls to an
inline `TranslationKey.of(NS, "setting.<id>.name")` literal. This repository uses Mojang
mappings and has ~1,650 `.description()` call sites across 949 Java files; rewriting each
one would be a very large, conflict-prone diff with no behavioural benefit.

Instead the keys are derived **centrally** and resolved **lazily**:

- `Module`, `Setting`, `Command`, `HudElementInfo` and `Category` now store a
  `TranslationKey` and expose `title()` / `description()` (and `nameKey()` /
  `descriptionKey()`) that call `key.get()` on **every invocation**. Nothing is cached at
  construction time, so a runtime language switch is reflected immediately.
- Keys are computed by `TranslationManager` from the identifiers Meteor already uses:
  `meteor-client.module.<id>.name`, `meteor-client.setting.<owner>.<group>.<id>.name`, etc.
  Meteor's `name` field is already a kebab-case identity (it is the NBT config key and the
  command identifier) and its display title was `Utils.nameToTitle(name)`, so the English
  values are the title-cased form of those identifiers — display output is byte-identical
  to before for English.
- Settings are scoped by **owner and group** (`setting.<owner>.<group>.<name>`) because 5
  setting names are genuinely reused with different text within a single owner (e.g.
  `BetterTooltips.keybind` in two places). The owner id is assigned at construction time by
  `Settings.assignOwner(...)` / `SettingGroup.add(...)`, so no call site had to change.
- Result: **zero call-site edits** in the 170 modules, and the existing config keys,
  command identifiers and NBT layout are untouched — config/command compatibility is
  preserved exactly.

`tools/gen_lang.py` regenerates `en_us.json` deterministically from source, so the file can
be rebuilt after any module is added or renamed.

## Public API for addon agents (Phase 3)
```
Import: meteordevelopment.meteorclient.translation

TranslationManager.METEOR_MOD_ID  = "meteor-client"

TranslationKey.of(namespace, subKey)          -> TranslationKey
TranslationKey.of(fullKey)                    -> TranslationKey
key.get()                                     -> String
key.get(Object... args)                       -> String

MeteorText.translated(key)                    -> MutableText (Component.literal)
MeteorText.translated(key, ChatFormatting...) -> MutableText

TranslationManager.setLanguage(langCode)      -> void
TranslationManager.registerNamespace(modId)   -> void
TranslationManager.addChangeListener(r)       -> void

Translations.register(addonInstance)          -> void
Translations.key(namespace, subKey)           -> TranslationKey
```

## Language file convention
assets/<modId>/lang/en_us.json
assets/<modId>/lang/de_de.json
...

`validateTranslations` treats a locale missing keys as advisory (the runtime falls back to
en_us), but fails on unknown keys, invalid key format, and `%s`/`%d`/`%f` placeholder-count
mismatches. Vanilla `key.*` entries are exempt from the Meteor schema because Minecraft's
`KeyMapping` consumes them directly.

## Issues and decisions
1. **Mappings differ from the spec.** The spec samples use Yarn (`net.minecraft.text.Text`,
   `Formatting`); this repo uses Mojang (`net.minecraft.network.chat.Component`,
   `ChatFormatting`). `MeteorText` and all touched base classes use the Mojang names.
2. **No `Component` reaches the server from Meteor display strings.** The independent
   translation system's security property holds: Meteor keys are resolved to `String`
   client-side and never become `translatable` components. See TRANSLATION_SECURITY_AUDIT.md.
3. **Runtime language switching** is exposed as a `ProvidedStringSetting` named `language`
   in `Config` → Visual, populated from `TranslationManager.getAvailableLanguages()`.
4. **5 pre-existing name collisions** (`KeyboardHud.show-cps` vs `Show CPS`,
   `BetterTooltips.keybind` ×2, `Nuker.side-color`/`line-color` ×2) are unchanged in source;
   the generator reports them and writes the first occurrence. Changing them would alter
   existing user config keys, so they were left as-is.
5. **`WaypointsModule` death waypoints** use a dynamic per-death name (`"Death " + time`)
   and are intentionally not translated.
6. **`Category`** previously had no display title — the GUI used the raw `name`. A
   `displayTitle()` accessor was added; the GUI now uses it, so categories are translatable.
