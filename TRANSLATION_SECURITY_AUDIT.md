# Translation Security Audit

Audit of every site where a Meteor- or addon-derived `Component` could reach the server,
performed against the `feature/independent-translation-system` branch.

## Threat model

Meteor's translation system must never emit `Component.translatable("<meteor-key>")` into
anything that is serialized to a server. A translatable component sends the *key* (not the
resolved text) over the wire; a server-controlled language file could then redefine
`meteor-client.*` keys, and the client would render attacker-chosen text. This system
therefore resolves every Meteor key to a plain literal client-side and only ever emits
`Component.literal(...)`.

## Audited sites

### 1. `Component.translatable` / `translatableEscape` call sites (17)

| File | Line | Namespace | Status | Notes |
|---|---|---|---|---|
| commands/commands/NbtCommand.java | 43 | `arguments.item.malformed` (vanilla) | SAFE | Vanilla namespace, client-local command feedback |
| commands/commands/LocateCommand.java | 81, 111, 141 | `filled_map.*` (vanilla) | SAFE | Compared client-side to `ItemStack` name; never sent |
| commands/arguments/RegistryEntryReferenceArgumentType.java | 43, 46 | `argument.resource.*` (vanilla) | SAFE | Vanilla namespace |
| commands/arguments/BlockPosArgumentType.java | 71-73 | `argument.pos.*` (vanilla) | SAFE | Vanilla namespace |
| utils/tooltip/BundleTooltipComponent.java | 38 | `item.minecraft.bundle.full` (vanilla) | SAFE | Tooltip render only |
| utils/misc/ComponentMapReader.java | 36, 38, 40, 43 | `arguments.item.component.*` (vanilla) | SAFE | Vanilla namespace |
| utils/render/DisplayItemUtils.java | 43 | `item.getDescriptionId()` (vanilla item id) | SAFE | Item display name for GUI icon |
| systems/modules/render/BetterTooltips.java | 419 | `item.container.more_items` (vanilla) | SAFE | Tooltip render only |
| systems/modules/render/BetterTooltips.java | 445 | `effect.getDescriptionId()` (vanilla effect id) | SAFE | Tooltip render only |
| mixin/viafabricplus/GeneralSettingsMixin.java | 16 | (commented-out reference) | SAFE | Not compiled |

Every remaining `translatable` call uses a **vanilla** namespace. No call uses a
`meteor-client.*` or addon key. There is nothing to fix.

### 2. Server-bound packet construction (17)

| File | Site | Status | Notes |
|---|---|---|---|
| commands/commands/SayCommand.java | 43-44 | SAFE | Payload is the user's raw command input; no `Component` involved |
| commands/commands/SwarmCommand.java | 137-361 | SAFE | Payload is user input / `getInput()`; `String`, not `Component` |
| utils/player/ChatUtils.java | 82-83 | SAFE | `sendPlayerMsg` forwards caller-supplied `String` |
| systems/modules/world/AutoSign.java | 77, 86 | SAFE | Sign lines come from the player's own sign text |
| systems/modules/misc/BookBot.java | 343 | SAFE | Book pages are `List<String>` from user config |
| gui/screens/EditBookTitleAndAuthorScreen.java | 57 | SAFE | Book pages/title are user-typed `String`s |
| utils/network/PacketUtils.java | 135, 152, 191 | SAFE | Packet-name → type map for the packet logger; no payload |

No server-bound site carries a `Component` built from a Meteor key. Meteor's own
`ChatUtils.sendMsg` renders client-side into the chat HUD and does not transmit.

### 3. Meteor display-string pipeline

| Layer | Mechanism | Status |
|---|---|---|
| `Module.title()` / `Module.description()` | `TranslationKey.get()` → `String` | SAFE — resolves to a literal, never a translatable component |
| `Setting.title()` / `Setting.description()` | `TranslationKey.get()` → `String` | SAFE |
| `Command.getTitle()` / `getDescription()` | `TranslationKey.get()` → `String` | SAFE |
| `HudElementInfo.title()` / `description()` | `TranslationKey.get()` → `String` | SAFE |
| `Category.displayTitle()` | `TranslationKey.get()` → `String` | SAFE |
| `MeteorText.translated(...)` | `Component.literal(key.get())` | SAFE by construction — the only sanctioned component factory |

`MeteorText` exists specifically so addons have a component-producing helper that cannot
leak a key. Core Meteor code passes display strings as `String` (chat, GUI labels,
tooltips), so it does not need to call `MeteorText`; the 0 direct core call sites are
expected rather than a gap.

## Summary

- Total sites audited: 34
- Fixed: 0 (no leaking site found)
- Remaining unresolved items: 0
