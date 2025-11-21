# CLAUDE.md - AI Assistant Guide for Meteor Client

This document provides a comprehensive guide for AI assistants working on the Meteor Client codebase. It covers the architecture, conventions, and best practices for making changes.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Codebase Structure](#codebase-structure)
3. [Key Architectural Patterns](#key-architectural-patterns)
4. [Development Workflows](#development-workflows)
5. [Code Style and Conventions](#code-style-and-conventions)
6. [Common Tasks](#common-tasks)
7. [Important Notes](#important-notes)

---

## Project Overview

**Project Name:** Meteor Client
**Repository:** gooner-client (fork of MeteorDevelopment/meteor-client)
**Purpose:** Minecraft Fabric utility mod for anarchy servers
**Language:** Java 21
**Build System:** Gradle with Kotlin DSL
**License:** GNU General Public License v3.0
**Minecraft Version:** 1.21.10
**Fabric Loader:** 0.17.3

### Project Scale
- **~940 Java source files**
- **197+ modules** across 6 categories
- **202+ mixins** for game modifications
- **68+ event types** in the event system
- **38+ commands** with Brigadier integration
- **36+ setting types** for configuration

### Key Dependencies
- **Orbit** (0.2.4): Custom event bus
- **Starscript** (0.2.5): Text formatting scripting
- **Discord IPC** (1.1): Rich Presence integration
- **Baritone**: Pathfinding integration (compileOnly)
- **Sodium, Lithium, Iris**: Performance/rendering mod compatibility
- **ViaFabricPlus**: Multi-version support compatibility

---

## Codebase Structure

### Root Directory Layout

```
gooner-client/
├── src/main/java/meteordevelopment/meteorclient/   # Main source
├── src/main/resources/                             # Resources
│   ├── fabric.mod.json                            # Mod metadata
│   ├── meteor-client.mixins.json                  # Core mixins
│   ├── meteor-client-{mod}.mixins.json           # Compat mixins
│   ├── meteor-client.accesswidener               # Access widener
│   └── assets/meteor-client/                     # Assets, fonts, lang
├── launch/                                        # Java 8 launcher subproject
├── build.gradle.kts                              # Main build script
├── settings.gradle.kts                           # Multi-project setup
├── gradle/libs.versions.toml                     # Version catalog
└── gradle.properties                             # Build properties
```

### Main Source Structure

All source code is under `src/main/java/meteordevelopment/meteorclient/`:

```
meteordevelopment/meteorclient/
├── MeteorClient.java          # Main entry point (ClientModInitializer)
├── MixinPlugin.java           # Mixin configuration plugin
├── ModMenuIntegration.java    # Mod menu integration
│
├── addons/                    # Addon system for extensibility
│   ├── AddonManager.java
│   └── MeteorAddon.java
│
├── commands/                  # Command system (38+ commands)
│   ├── Command.java          # Base command class
│   ├── Commands.java         # Command registry
│   ├── arguments/            # Custom argument types
│   └── commands/             # Command implementations
│
├── events/                    # Event system (68+ events)
│   ├── Cancellable.java      # Base cancellable event
│   ├── entity/               # Entity events
│   ├── game/                 # Game lifecycle events
│   ├── meteor/               # Client-specific events
│   ├── packets/              # Network packet events
│   ├── render/               # Rendering events
│   └── world/                # World events
│
├── gui/                       # GUI system (133+ files)
│   ├── GuiThemes.java
│   ├── WidgetScreen.java
│   ├── renderer/             # GUI rendering engine
│   ├── screens/              # Screen implementations
│   ├── tabs/                 # Tab system
│   ├── themes/               # Theme system
│   └── widgets/              # Widget library
│
├── mixin/                     # Mixins (202+)
│   ├── (minecraft packages)  # Core game mixins
│   ├── baritone/             # Baritone compatibility
│   ├── indigo/               # Indigo renderer compat
│   ├── lithium/              # Lithium compat
│   ├── sodium/               # Sodium compat
│   └── viafabricplus/        # ViaFabricPlus compat
│
├── mixininterface/            # Mixin accessor interfaces
│
├── pathing/                   # Pathfinding utilities
│
├── renderer/                  # Custom rendering system
│   ├── Renderer2D.java
│   ├── Renderer3D.java
│   ├── Fonts.java
│   └── text/                 # Text rendering
│
├── settings/                  # Settings system (36+ types)
│   ├── Setting.java          # Base setting
│   ├── Settings.java         # Settings container
│   ├── SettingGroup.java     # Setting group
│   ├── BoolSetting.java
│   ├── IntSetting.java
│   ├── DoubleSetting.java
│   ├── EnumSetting.java
│   ├── BlockListSetting.java
│   └── [many more...]
│
├── systems/                   # Core systems architecture
│   ├── System.java           # Base system class
│   ├── Systems.java          # System registry & lifecycle
│   ├── accounts/             # Account management
│   ├── config/               # Configuration system
│   ├── friends/              # Friends system
│   ├── hud/                  # HUD system
│   ├── macros/               # Macro system
│   ├── modules/              # Module system (197+ modules)
│   │   ├── Module.java       # Base module class
│   │   ├── Modules.java      # Module registry
│   │   ├── Categories.java   # Module categories
│   │   ├── combat/           # Combat modules
│   │   ├── misc/             # Miscellaneous modules
│   │   ├── movement/         # Movement modules
│   │   ├── player/           # Player modules
│   │   ├── render/           # Render modules (37+)
│   │   └── world/            # World interaction modules
│   ├── profiles/             # Profile system
│   ├── proxies/              # Proxy management
│   └── waypoints/            # Waypoint system
│
└── utils/                     # Utility packages
    ├── entity/               # Entity utilities
    ├── files/                # File I/O utilities
    ├── misc/                 # Miscellaneous utilities
    ├── network/              # Network utilities
    ├── notebot/              # Noteblock bot system
    ├── player/               # Player utilities
    ├── render/               # Render utilities
    ├── tooltip/              # Tooltip utilities
    └── world/                # World utilities
```

### Important Entry Points

| File | Purpose | Line Reference |
|------|---------|----------------|
| `MeteorClient.java` | Main mod initializer, sets up event bus, systems, keybinds | Main entry point |
| `Systems.java` | Central registry for all systems with lifecycle management | System access |
| `Modules.java` | Module registry, category management, keybind handling | Module access |
| `Commands.java` | Command registry using Brigadier | Command access |
| `GuiThemes.java` | Theme registry for GUI customization | Theme access |

### Key Static Accessors

Use these throughout the codebase to access common functionality:

```java
MeteorClient.mc              // MinecraftClient instance
MeteorClient.EVENT_BUS       // Event bus instance
MeteorClient.FOLDER          // Mod config folder
MeteorClient.LOG             // Logger instance

Systems.get(Class<T>)        // Get system by class
Modules.get()                // Get Modules system
Config.get()                 // Get Config system
```

---

## Key Architectural Patterns

### 1. Event-Driven Architecture

Uses **Orbit event bus** with lambda-based handlers:

```java
// Listening to events
@EventHandler
private void onTick(TickEvent.Pre event) {
    // Handle tick
}

// Firing events
MeteorClient.EVENT_BUS.post(new MyEvent());

// Cancellable events
if (event instanceof Cancellable) {
    ((Cancellable) event).cancel();
}
```

**Event categories:**
- `events/entity/` - Entity-related events
- `events/game/` - Game lifecycle (OpenScreenEvent, WindowResizedEvent)
- `events/meteor/` - Client-specific (KeyEvent, MouseClickEvent)
- `events/packets/` - Network packets (PacketEvent)
- `events/render/` - Rendering (Render2DEvent, Render3DEvent)
- `events/world/` - World events (TickEvent, ChunkDataEvent)

### 2. Module System

**All modules:**
- Extend `Module.java` base class
- Are toggle-able features with on/off states
- Belong to a category (Combat, Player, Movement, Render, World, Misc)
- Have settings for configuration
- Can listen to events
- Support keybinds

**Module lifecycle:**
```java
public class MyModule extends Module {
    public MyModule() {
        super(Categories.World, "my-module", "Description");
    }

    @Override
    public void onActivate() {
        // Called when module is enabled
    }

    @Override
    public void onDeactivate() {
        // Called when module is disabled
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Handle events when active
    }
}
```

**Module categories:**
- **Combat**: PvP-focused (KillAura, AutoArmor, AutoCity, Criticals)
- **Player**: Player enhancements (AutoTool, Freecam, FakePlayer, AutoEat)
- **Movement**: Movement mods (Fly, Speed, NoFall, Sprint, Step)
- **Render**: Visual mods (ESP, Tracers, Nametags, Fullbright, XRay)
- **World**: World interaction (AutoMine, Scaffold, Timer, LiquidFiller)
- **Misc**: Other features (Discord RPC, AutoReconnect, Notifier)

### 3. Settings System

**Settings are type-safe and GUI-integrated:**

```java
public class MyModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> myBool = sgGeneral.add(new BoolSetting.Builder()
        .name("my-bool")
        .description("Description")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> myInt = sgGeneral.add(new IntSetting.Builder()
        .name("my-int")
        .description("Description")
        .defaultValue(5)
        .min(1)
        .max(10)
        .sliderRange(1, 10)
        .build()
    );

    private final Setting<Mode> myEnum = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Description")
        .defaultValue(Mode.Value1)
        .build()
    );

    // Access setting values
    public void example() {
        boolean value = myBool.get();
        int intValue = myInt.get();
    }
}
```

**Common setting types:**
- `BoolSetting` - Boolean checkbox
- `IntSetting` - Integer with slider
- `DoubleSetting` - Double with slider
- `EnumSetting` - Enum dropdown
- `StringSetting` - Text input
- `ColorSetting` - Color picker
- `BlockListSetting` - Block selection list
- `EntityTypeListSetting` - Entity type list
- `KeybindSetting` - Keybind input
- `ItemListSetting` - Item selection list

### 4. System Pattern

**Systems are persistent, stateful managers:**

```java
public class MySystem extends System<MySystem> {
    public MySystem() {
        super("my-system");
    }

    @Override
    public void init() {
        // Initialize system
    }

    @Override
    public void save(File folder) {
        // Save state to disk
    }

    @Override
    public void load(File folder) {
        // Load state from disk
    }
}

// Access via Systems registry
MySystem system = Systems.get(MySystem.class);
```

**Core systems:**
- `Modules` - Module registry
- `Config` - Global configuration
- `HUD` - HUD elements
- `Friends` - Friend list
- `Accounts` - Alt accounts
- `Macros` - Keybind macros
- `Profiles` - Config profiles
- `Waypoints` - Coordinate waypoints
- `Proxies` - SOCKS proxies

### 5. Command Pattern

**Commands use Mojang's Brigadier framework:**

```java
public class MyCommand extends Command {
    public MyCommand() {
        super("mycommand", "Description", "alias1", "alias2");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("subcommand")
            .then(argument("value", IntegerArgumentType.integer())
                .executes(context -> {
                    int value = context.getArgument("value", Integer.class);
                    info("Value: " + value);
                    return SINGLE_SUCCESS;
                })
            )
        );
    }
}
```

### 6. Mixin Pattern

**Mixins inject code into Minecraft:**

```java
@Mixin(MinecraftClass.class)
public abstract class MinecraftClassMixin {
    // Inject at method head
    @Inject(method = "methodName", at = @At("HEAD"), cancellable = true)
    private void onMethodName(CallbackInfo info) {
        MyEvent event = new MyEvent();
        MeteorClient.EVENT_BUS.post(event);

        if (event.isCancelled()) {
            info.cancel();
        }
    }

    // Modify return value
    @Inject(method = "methodName", at = @At("RETURN"), cancellable = true)
    private void onMethodNameReturn(CallbackInfoReturnable<Type> info) {
        info.setReturnValue(newValue);
    }

    // Redirect method call
    @Redirect(method = "methodName", at = @At(value = "INVOKE", target = "..."))
    private ReturnType redirectMethod(TargetClass instance, Args args) {
        return modifiedValue;
    }
}
```

**Mixin accessors (in mixininterface/):**

```java
public interface IMinecraftClass {
    @Accessor("privateField")
    Type getPrivateField();

    @Accessor("privateField")
    void setPrivateField(Type value);

    @Invoker("privateMethod")
    ReturnType invokePrivateMethod(Args args);
}

// Usage
IMinecraftClass accessor = (IMinecraftClass) minecraftInstance;
Type value = accessor.getPrivateField();
```

### 7. GUI Widget System

**Custom widget-based GUI:**

```java
public class MyScreen extends WidgetScreen {
    public MyScreen(GuiTheme theme) {
        super(theme, "My Screen");
    }

    @Override
    public void initWidgets() {
        WWindow window = add(theme.window("Window Title")).widget();

        window.add(theme.label("Label text")).expandX();
        window.add(theme.button("Button")).widget().action = () -> {
            info("Button clicked!");
        };

        WTextBox textBox = window.add(theme.textBox("")).expandX().widget();
        textBox.action = () -> {
            String text = textBox.get();
        };
    }
}
```

---

## Development Workflows

### Building

```bash
./gradlew build
```

**Build process:**
1. Compiles main project (Java 21)
2. Compiles launch subproject (Java 8)
3. Processes resources (version substitution)
4. Generates JAR with manifest
5. Includes JAR-in-JAR dependencies
6. Applies access widener

**Output:** `build/libs/meteor-client-{version}.jar`

### Running in Development

```bash
./gradlew runClient
```

Or use IntelliJ IDEA run configurations created by Fabric Loom.

### Generating Sources

```bash
./gradlew genSources
```

Decompiles Minecraft for IDE navigation and debugging.

### Migrating Mappings

When Minecraft updates with new mappings:

```bash
./gradlew migrateMappings --mappings "1.21.10+build.2"
```

Updates source code to new Yarn mappings.

### Publishing

**CI/CD (GitHub Actions):**
- `.github/workflows/build.yml` - Automated builds on push
- `.github/workflows/pull-request.yml` - PR validation
- Publishes snapshots to `maven.meteordev.org`

**Version format:** `{minecraft-version}-{build-number}`

### Testing

**No formal test structure** - testing is manual:
1. Run in dev environment
2. Test specific features
3. Check for crashes/errors
4. Test mod compatibility

---

## Code Style and Conventions

### License Header

**ALL Java files MUST include this header:**

```java
/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
```

### Code Style

**From README.md contributions section:**
- Check existing code to match style
- **Favor readability over compactness**
- Reference: [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)

**From .editorconfig:**
- **Charset:** UTF-8
- **Indent style:** Spaces
- **Indent size:** 4 spaces
- **Insert final newline:** Yes
- **Trim trailing whitespace:** Yes
- **JSON/YAML indent:** 2 spaces

### Naming Conventions

**Packages:** `lowercase.without.underscores`
**Classes:** `PascalCase`
**Methods:** `camelCase`
**Variables:** `camelCase`
**Constants:** `UPPER_SNAKE_CASE`
**Private fields:** `camelCase` (not prefixed with underscore)

**Module naming:**
```java
super(Categories.World, "module-name", "Description");
// Module ID uses kebab-case
```

**Setting naming:**
```java
.name("setting-name")  // kebab-case
```

### Package Organization

**Visibility:**
- `public` - Public API, used across packages
- `package-private` - Implementation details within package
- `private` - Internal to class

**Util classes:**
```java
public final class Utils {
    private Utils() {}  // Prevent instantiation

    public static void method() {}
}
```

### Common Patterns

**Checking if in-game:**
```java
if (!Utils.canUpdate()) return;  // Checks if world and player are not null
```

**Sending chat messages:**
```java
import meteordevelopment.meteorclient.utils.player.ChatUtils;

ChatUtils.info("Info message");
ChatUtils.warning("Warning message");
ChatUtils.error("Error message");
```

**Accessing player/world:**
```java
import static meteordevelopment.meteorclient.MeteorClient.mc;

mc.player      // Client player
mc.world       // Client world
mc.options     // Game options
mc.getWindow() // Game window
```

**Block interaction:**
```java
import meteordevelopment.meteorclient.utils.world.BlockUtils;

BlockUtils.place(blockPos, Hand.MAIN_HAND, slot, rotate, packets, true);
BlockUtils.breakBlock(blockPos, swing);
```

**Player rotation:**
```java
import meteordevelopment.meteorclient.utils.player.Rotations;

Rotations.rotate(yaw, pitch);
Rotations.rotate(pos, () -> {
    // Code executed after rotating to pos
});
```

**Rendering:**
```java
@EventHandler
private void onRender3D(Render3DEvent event) {
    event.renderer.box(pos, color, lineColor, shapeMode, 0);
    event.renderer.line(x1, y1, z1, x2, y2, z2, color);
}

@EventHandler
private void onRender2D(Render2DEvent event) {
    event.renderer.text("Text", x, y, color);
    event.renderer.quad(x, y, width, height, color);
}
```

---

## Common Tasks

### Adding a New Module

1. **Create module class** in appropriate category package:

```java
package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class MyModule extends Module {
    public MyModule() {
        super(Categories.World, "my-module", "Module description");
    }
}
```

2. **Module auto-registers** via `@ReflectInit` annotation on Categories class

3. **Add settings** if needed:

```java
private final SettingGroup sgGeneral = settings.getDefaultGroup();

private final Setting<Boolean> mySetting = sgGeneral.add(new BoolSetting.Builder()
    .name("my-setting")
    .description("Setting description")
    .defaultValue(true)
    .build()
);
```

4. **Add event handlers:**

```java
@EventHandler
private void onTick(TickEvent.Pre event) {
    // Module logic
}
```

### Adding a New Command

1. **Create command class** in `commands/commands/`:

```java
package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;

public class MyCommand extends Command {
    public MyCommand() {
        super("mycommand", "Command description");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            info("Command executed!");
            return SINGLE_SUCCESS;
        });
    }
}
```

2. **Command auto-registers** via reflection

### Adding a New Event

1. **Create event class** in appropriate package:

```java
package meteordevelopment.meteorclient.events.world;

public class MyEvent {
    private static final MyEvent INSTANCE = new MyEvent();

    public static MyEvent get() {
        return INSTANCE;
    }
}
```

2. **For cancellable events:**

```java
import meteordevelopment.meteorclient.events.Cancellable;

public class MyEvent extends Cancellable {
    private static final MyEvent INSTANCE = new MyEvent();

    public static MyEvent get() {
        return INSTANCE;
    }
}
```

3. **Fire event** in mixin or other code:

```java
MeteorClient.EVENT_BUS.post(MyEvent.get());
```

4. **Listen to event:**

```java
@EventHandler
private void onMyEvent(MyEvent event) {
    // Handle event
}
```

### Adding a New Setting Type

1. **Create setting class** in `settings/`:

```java
package meteordevelopment.meteorclient.settings;

public class MyCustomSetting extends Setting<MyType> {
    public MyCustomSetting(String name, String description, MyType defaultValue, Consumer<MyType> onChanged, Consumer<Setting<MyType>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    protected MyType parseImpl(String str) {
        // Parse from string
    }

    @Override
    protected boolean isValueValid(MyType value) {
        // Validate value
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return null; // Or provide suggestions
    }

    @Override
    protected NbtCompound save(NbtCompound tag) {
        // Serialize to NBT
    }

    @Override
    protected MyType load(NbtCompound tag) {
        // Deserialize from NBT
    }

    public static class Builder extends SettingBuilder<Builder, MyType, MyCustomSetting> {
        @Override
        public MyCustomSetting build() {
            return new MyCustomSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
```

2. **Add widget renderer** in `gui/themes/meteor/widgets/`:

```java
public class WMyCustomSetting extends WWidget {
    // Widget implementation for GUI
}
```

### Adding a Mixin

1. **Create mixin class** in appropriate package:

```java
package meteordevelopment.meteorclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo info) {
        // Mixin code
    }
}
```

2. **Register in mixin config** (`src/main/resources/meteor-client.mixins.json`):

```json
{
  "mixins": [
    "MinecraftClientMixin"
  ]
}
```

3. **For accessor mixins**, create interface:

```java
package meteordevelopment.meteorclient.mixininterface;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface IMinecraftClient {
    @Accessor("privateField")
    Type getPrivateField();
}
```

### Adding Mod Compatibility

1. **Create compat mixin** in `mixin/{modname}/`

2. **Create compat mixin config** (`meteor-client-{modname}.mixins.json`)

3. **Update MixinPlugin.java** to conditionally load:

```java
if (FabricLoader.getInstance().isModLoaded("modid")) {
    mixinConfigs.add("meteor-client-modname.mixins.json");
}
```

### Accessing Private Fields/Methods

1. **Create accessor interface** in `mixininterface/`:

```java
@Mixin(TargetClass.class)
public interface ITargetClass {
    @Accessor("fieldName")
    Type getFieldName();

    @Invoker("methodName")
    ReturnType invokeMethodName(Args args);
}
```

2. **Use in code:**

```java
ITargetClass accessor = (ITargetClass) targetInstance;
Type value = accessor.getFieldName();
```

3. **Or use access widener** (`src/main/resources/meteor-client.accesswidener`):

```
accessWidener v2 named

accessible field net/minecraft/class/path/ClassName fieldName Lnet/minecraft/class/path/Type;
accessible method net/minecraft/class/path/ClassName methodName ()V
```

---

## Important Notes

### Things to Remember

1. **License header is MANDATORY** on all Java files
2. **Always favor readability over compactness**
3. **Check `Utils.canUpdate()`** before accessing player/world
4. **Use event system** instead of polling where possible
5. **Settings are accessed via `.get()`**, not directly
6. **Module IDs use kebab-case**, class names use PascalCase
7. **All mods are for educational/anarchy servers** - respect Mojang's EULA
8. **GPL-3.0 license** - all derivatives must be open source
9. **Java 21 target** - use modern Java features
10. **Test with Sodium/Lithium/Iris** - compatibility is important

### Common Pitfalls

1. **Forgetting null checks:**
   ```java
   // BAD
   mc.player.getName();

   // GOOD
   if (Utils.canUpdate()) {
       mc.player.getName();
   }
   ```

2. **Not cancelling events properly:**
   ```java
   // BAD
   event.cancel();  // Only works if event extends Cancellable

   // GOOD
   if (event instanceof Cancellable) {
       ((Cancellable) event).cancel();
   }
   ```

3. **Hardcoding values instead of settings:**
   ```java
   // BAD
   private static final int DELAY = 5;

   // GOOD
   private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
       .name("delay")
       .defaultValue(5)
       .build()
   );
   ```

4. **Not using static accessors:**
   ```java
   // BAD
   Systems.INSTANCE.get(Modules.class)

   // GOOD
   Systems.get(Modules.class)
   // Or even better
   Modules.get()
   ```

5. **Forgetting to unsubscribe from events:**
   - Modules auto-subscribe/unsubscribe when toggled
   - Manual event listeners need cleanup

### Performance Considerations

1. **Minimize work in tick events** - runs 20 times per second
2. **Cache expensive lookups** - don't recalculate every frame
3. **Use Pre events for cancellation** - avoid unnecessary processing
4. **Batch rendering** - use renderer methods efficiently
5. **Avoid blocking operations** - use async where appropriate

### Debugging Tips

1. **Use MeteorClient.LOG:**
   ```java
   MeteorClient.LOG.info("Debug message");
   MeteorClient.LOG.error("Error message", exception);
   ```

2. **Check mixin environment:**
   ```java
   MixinEnvironment.getCurrentEnvironment().audit();
   ```

3. **Use chat output for quick debugging:**
   ```java
   ChatUtils.info("Debug: " + value);
   ```

4. **Check module is active:**
   ```java
   if (!isActive()) return;
   ```

### File Locations

**Config folder:** `~/.minecraft/meteor-client/`
**Modules config:** `meteor-client/modules.nbt`
**Accounts:** `meteor-client/accounts.nbt`
**Waypoints:** `meteor-client/waypoints.nbt`
**Macros:** `meteor-client/macros.nbt`
**Profiles:** `meteor-client/profiles/`

### Useful Links

- **Main Repository:** https://github.com/MeteorDevelopment/meteor-client
- **Discord:** https://discord.gg/bBGQZvd
- **Wiki:** https://meteorclient.com/faq
- **Fabric Docs:** https://fabricmc.net/wiki
- **Mixin Docs:** https://github.com/SpongePowered/Mixin/wiki
- **Brigadier:** https://github.com/Mojang/brigadier

---

## Quick Reference

### Key Classes to Know

| Class | Purpose | Package |
|-------|---------|---------|
| `MeteorClient` | Main entry point, event bus, static accessors | Root |
| `Module` | Base class for all modules | `systems.modules` |
| `System` | Base class for all systems | `systems` |
| `Command` | Base class for all commands | `commands` |
| `Setting` | Base class for all settings | `settings` |
| `Utils` | Common utilities and canUpdate() | `utils` |
| `ChatUtils` | Chat message output | `utils.player` |
| `BlockUtils` | Block interaction utilities | `utils.world` |
| `PlayerUtils` | Player utilities | `utils.player` |
| `Rotations` | Player rotation utilities | `utils.player` |
| `Renderer2D/3D` | Rendering utilities | `renderer` |

### Event Priority

Events are processed in priority order:
- `EventPriority.HIGHEST` - First to run
- `EventPriority.HIGH`
- `EventPriority.MEDIUM` (default)
- `EventPriority.LOW`
- `EventPriority.LOWEST` - Last to run

```java
@EventHandler(priority = EventPriority.HIGH)
private void onEvent(MyEvent event) { }
```

### Module Categories

```java
Categories.Combat     // PvP features
Categories.Player     // Player enhancements
Categories.Movement   // Movement modifications
Categories.Render     // Visual modifications
Categories.World      // World interaction
Categories.Misc       // Miscellaneous
```

### Common Imports

```java
// Core
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Categories;

// Events
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;

// Settings
import meteordevelopment.meteorclient.settings.*;

// Utils
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;

// Minecraft
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import static meteordevelopment.meteorclient.MeteorClient.mc;
```

---

## Conclusion

This guide covers the essential information for working on the Meteor Client codebase. The project is well-architected with clear separation of concerns, extensive use of the event system, and a powerful module framework. When in doubt:

1. Check existing similar code
2. Follow the established patterns
3. Maintain readability
4. Test thoroughly
5. Respect the GPL-3.0 license

Happy coding!
