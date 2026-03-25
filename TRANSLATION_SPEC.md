# Meteor Client Translation Key Specification

---

## Overview

Our translation system uses hierarchical, dot-separated keys to organize translatable strings across commands, modules, 
UI, and configuration. Keys follow a deterministic structure that enables both core and addon developers to maintain 
consistency while avoiding hardcoded strings.

See PR [#6029](https://github.com/MeteorDevelopment/meteor-client/pull/6029) for the implementation and complete 
migration from the old hardcoded format.

## Motivation

The primary motivation was to finally allow Meteor to be properly localised. We have fixed errors in the font system
to allow it to render arbitrary Unicode characters, added a language engine and refactored almost all user-facing parts
of the codebase to accept translatable strings instead of hardcoded ones.

It also comes with the benefit of patching the translation exploit, which allowed servers to detect that players were
using meteor, even with no modules enabled. [See here](https://wurst.wiki/sign_translation_vulnerability) for more 
information.

---

## Key Format

### General Structure

```
{namespace}.{entity}[.{sub-entity}]
```

All segments use **kebab-case** (hyphens between words, no underscores except where noted).

### Namespace Categories

| Namespace    | Purpose                               | Examples                                         |
|--------------|---------------------------------------|--------------------------------------------------|
| `meteor`     | Core system (keybinds, language info) | `meteor.key.open-gui`, `meteor.lang.translators` |
| `command`    | Commands and CLI features             | `command.friends`, `command.give`                |
| `module`     | Gameplay/visual modules               | `module.auto-fish`, `module.esp`                 |
| `category`   | Module classification                 | `category.combat`, `category.render`             |
| `config`     | Configuration section headers         | `config.chat`, `config.modules`                  |
| `hud`        | HUD widget settings and labels        | `hud.armor`, `hud.potion-timers`                 |
| `tab`        | UI tab names                          | `tab.config`, `tab.modules`                      |
| `profile`    | Profile system strings                | `profile.general`, `profile.save`                |
| `proxy`      | Proxy configuration                   | `proxy.general`, `proxy.optional`                |
| `setting`    | Generic setting types                 | `setting.blockpos`, `setting.group`              |
| `marker`     | Marker types (visual aids)            | `marker.cuboid`, `marker.sphere-2d`              |
| `waypoint`   | Waypoint system                       | `waypoint.position`, `waypoint.visual`           |
| `macro`      | Macro system                          | `macro.general`                                  |
| `text`       | UI text/labels                        | `text.*`                                         |
| `theme`      | UI themes                             | `theme.meteor`                                   |
| `starscript` | Starscript system                     | `starscript.title`                               |

### Prefix Behavior

Keys are looked up as-is, with no automatic prefix modification:

```java
MeteorTranslations.translate("module.auto-fish");      // looks up "module.auto-fish"
MeteorTranslations.translate("command.friends");       // looks up "command.friends"
MeteorTranslations.translate("meteor.key.open-gui");   // looks up "meteor.key.open-gui"
```

The namespace prefix is part of the key itself. Choose consistent prefixes based on context: `meteor.*`, `command.*`, 
`module.*`, etc.

---

## Entity and Sub-Entity Naming

### Command Keys

```
command.{name}                                  // Command name/title
command.{name}.description                      // Short description (shown in help)
command.{name}.error.{error-name}               // Error messages used in the provided error method
command.{name}.warning.{warning-name}           // Warning messages used in the provided warning method
command.{name}.info.{info-type}                 // Info messages used in the provided info method
command.{name}.exception.{exception-name}       // Command exceptions
```

**Examples:**
```
command.friends
command.friends.description
command.give.exception.no_space
```

### Module Keys

```
module.{name}                                           // Module name/title
module.{name}.description                               // Brief description of module
module.{name}.{category}.{setting-name}                 // Setting within a category
module.{name}.{category}.{setting-name}.description     // Setting description
```

**Examples:**
```
module.auto-fish
module.auto-fish.description
module.auto-fish.general.auto-switch
module.auto-fish.general.auto-switch.description
module.auto-fish.general.cast-delay
```

### Base Module Keys

The `module.base` namespace contains keys shared across all modules:

```
module.base.bind                        // Keybinding control label
module.base.bind.bind                   // Keybind assignment option
module.base.bind.toggle-on-release      // Toggle behavior option
module.base.bind.chat-feedback          // Option: show feedback in chat
module.base.active                      // "Active" label
module.base.toggled.on                  // Display when module is on
module.base.toggled.off                 // Display when module is off
module.base.bound                       // "Bound to [key]" indicator
module.base.unbound                     // "Not bound" indicator
module.base.copy-config                 // Button: copy configuration
module.base.paste-config                // Button: paste configuration
module.base.from                        // "Pasted from..." label
```

### Category Keys

Module categories for organization:

```
category.combat
category.movement
category.player
category.misc
category.render
category.world
```

Each should map to a human-readable category name in the translation file.

---

## Keybinding Keys

Keybindings use the `meteor.key` namespace:

```
meteor.key.category               // Category name for key group (e.g., "Meteor Client")
meteor.key.{action}               // Keybind display name
```

**Examples:**
```json
{
  "meteor.key.category": "Meteor Client",
  "meteor.key.open-gui": "Open GUI",
  "meteor.key.open-commands": "Open Commands"
}
```

These map to Minecraft's `KeyBinding` system and are translated via the `KeyBindingCategoryMixin` and 
`ControlListWidgetMixin`.

---

## Language Metadata

```
meteor.lang.translators           // Credit line for translation contributors
```

**Example:**
```json
{
  "meteor.lang.translators": "MeteorDevelopment"
}
```

---

## Using `MeteorTranslations.translate()`

```java
import meteordevelopment.meteorclient.utils.misc.MeteorTranslations;

// Basic translation
String msg = MeteorTranslations.translate("module.auto-fish");

// With String.format-style arguments
String msg = MeteorTranslations.translate("command.friends.info.added", playerName);

// With explicit String fallback
String msg = MeteorTranslations.translate("custom.key", "Default text");

// With String fallback and arguments
String msg = MeteorTranslations.translate(
    "command.locate.info.mansion",
    "Unknown location",
    x, y, z
);

// With lazy-evaluated fallback (Supplier)
String msg = MeteorTranslations.translate(
    "module.custom.setting",
    () -> "Computed default: " + computeDefault(),
    args
);
```

Fallback chain: current language -> en_us -> explicit fallback (if provided)

---

## Addon Developer Guidelines

### Loading Addon Language Files

Addon language files are automatically discovered and loaded. Place them at:

`src/main/resources/assets/{addon-id}/language/{lang_code}.json`

Example structure:
```
my-addon/
  src/main/resources/assets/my-addon/language/
    en_us.json
    de_de.json
    fr_fr.json
```

Contents of `en_us.json`:
```json
{
  "module.custom-esp": "Custom ESP",
  "module.custom-esp.description": "Advanced ESP renderer.",
  "module.custom-esp.general.range": "Render Distance"
}
```

---

## Formatting and Style

### Key Naming Rules

1. **Use kebab-case for multi-word segments**
    - YES: `auto-fish`, `toggle-on-release`, `cast-delay`
    - NO: `auto_fish`, `toggleOnRelease`, `castdelay`

2. **Use consistent entity names**
    - Match the actual command/module name in code
    - If module is `AutoFish`, key is `module.auto-fish`

3. **Keep keys hierarchically shallow** (3-5 levels typical)
    - YES: `module.auto-fish.general.cast-delay`
    - NO: `module.auto-fish.settings.general.behavior.timing.cast.delay`

### Value Guidelines

1. **Descriptions** (tooltip-style): Keep under 100 characters; be precise.
2. **Messages**: Use `%s` for placeholders (Minecraft-style formatting).
3. **Consistency**: Reuse standard terms across keys (e.g., "Range", "Distance").

**Example values:**
```json
{
  "module.auto-fish.description": "Automatically casts and catches fish.",
  "module.auto-fish.general.cast-delay": "Cast Delay",
  "module.auto-fish.general.cast-delay.description": "Delay in ms before casting.",
  "command.friends.info.added": "Added %s to friends.",
  "command.locate.error.cant_locate_monument": "Couldn't locate the monument!"
}
```

---

## Language File Structure

### File Location

```
src/main/resources/assets/meteor-client/language/{lang_code}.json
```

Language codes follow Minecraft's convention: `en_us`, `en_gb`, `fr_fr`, `de_de`, `zh_cn`, etc.

### File Format

A flat JSON object mapping keys to translated strings:

```json
{
  "meteor.key.category": "Meteor Client",
  "meteor.key.open-gui": "Open GUI",
  "module.auto-fish": "Auto Fish",
  "module.auto-fish.description": "Automatically casts and catches fish.",
  "command.friends": "Friends",
  "command.friends.description": "Manages friends.",
  "command.friends.error.already_friends": "Already friends with that player."
}
```

### Best Practices

- **Keep keys sorted** (e.g., within namespace groups for readability).
- **Ensure complete coverage** of keys for `en_us` (fallback language).
- **Validate JSON** before committing.

---

## Fallback Behavior

The translation system follows this resolution chain:

1. **Current language** (user's selected locale)
    - Check if key exists in current language map
2. **Default language** (always `en_us`)
    - Check if key exists in en_us map
3. **Explicit fallback** (if provided)
    - Return the provided fallback String or call Supplier
4. **Last resort**: Return the translation key itself

**Code reference:**
```java
public static String translate(String key) {
    MeteorLanguage currentLang = getCurrentLanguage();
    return currentLang.get(key, () -> getDefaultLanguage().get(key));
}

// With explicit fallback:
public static String translate(String key, String fallback) {
    MeteorLanguage currentLang = getCurrentLanguage();
    return currentLang.get(key, () -> getDefaultLanguage().get(key, fallback));
}
```

---

## Validation Checklist

Before submitting PR or release:

- All keys follow `{namespace}.{entity}[.{sub}]` pattern
- Keys use kebab-case for multi-word segments
- `en_us.json` is 100% complete (all keys present)
- No hardcoded English strings in source (use `MeteorTranslations.translate()`)
- New modules/commands documented in translation file
- JSON is valid and properly formatted

---

## Examples

### Full Command Example

```java
// Code: Command with arguments
public class FriendsCommand extends Command {
    private static final SimpleCommandExceptionType FRIEND_EXCEPTION = 
        new SimpleCommandExceptionType(MeteorClient.translatable("command.friends.exception.oops"));
    
    public FriendsCommand() {
        super("friends");
    }

    public void addFriend(String playerName) {
        info("added", playerName).send();
        
        if (!logic()) throw FRIEND_EXCEPTION.create();
    }
}

// JSON keys:
"command.friends": "Friends",
"command.friends.description": "Manages friends.",

"command.friends.error.already_friends": "Already friends with that player.",
"command.friends.error.failed": "Failed to remove that friend.",
"command.friends.error.not_friends": "Not friends with that player.",
"command.friends.info.added": "Added %s to friends.",
"command.friends.info.removed": "Removed %s from friends.",
"command.friends.info.friends": "--- Friends (%s) ---",

"command.friends.exception.oops": "Something went wrong.",
```

### Full Module Example

```java
// Code: Module with settings
public class ExampleModule extends Module {
    private final SettingGroup sgExample = settings.createGroup("example-setting-group");

    public Setting<Integer> value = sgExample.add(new IntSetting.Builder()
        .name("value")
        .min(1)
        .defaultValue(10)
        .build()
    );

    public ExampleModule() {
        super("example-module");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        info("random", value.get(), Math.random() * value.get());
    }
}

// JSON keys:
"setting.group.example-setting-group": "Example Setting Group",

"module.example-module": "Example Module",
"module.example-module.description": "Example module for the translations specification.",

"module.example-module.example-setting-group.value": "Value",
"module.example-module.example-setting-group.value.description": "Example value.",

"module.example-module.info.random": "The random value between 0 and %s is %s"
```

---

## See Also

- PR #6029 - https://github.com/MeteorDevelopment/meteor-client/pull/6029 - Complete implementation and translation 
  format migration
- `MeteorTranslations.java` - Core translation system implementation
- `LanguageManagerMixin.java` - Minecraft language integration
- `KeyBindingCategoryMixin.java` - Keybind category translation
- `ControlListWidgetMixin.java` - Control widget translation override
