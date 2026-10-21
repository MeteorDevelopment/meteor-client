# Minecraft version compatibility layer

Meteor Client is compiled against a single Minecraft version at a time. This document describes
the compatibility layer and the multi-version build that keep version-specific knowledge in one
place, so supporting another Minecraft version is a bounded change instead of a full-repo update.

## Goal

- The common code in `src/main/java` is version-agnostic and programs against `ICompat`.
- Version-specific code lives in `src/mc/<version>/` and is the only thing that changes per version.
- Each supported version is described by `gradle/mc-versions/<version>.properties`.
- The build produces one jar per version; the compatibility layer implementation is selected at
  runtime, so the common code has no compile-time dependency on a particular version.

## Layout

```
src/main/java/meteordevelopment/meteorclient/compat/
    ICompat.java          version-agnostic contract (no net.minecraft imports)
    Compat.java           resolves and holds the active implementation
    McVersion.java        comparable representation of a Minecraft version
    ScreenKind.java       version-agnostic classification of the open screen
src/main/java/meteordevelopment/meteorclient/mc/            (shared, version-agnostic)
    McTexture.java        opaque handle to a texture of a version-specific type
src/mc/<version>/meteordevelopment/meteorclient/mc/
    Vanilla.java          the version specific shims the shared code calls
    MixinTargets.java     the mixin target names that differ per version
src/main/resources/meteor-compat.properties   template; the impl class is substituted at build time
src/mc/26.2/              sources compiled only for Minecraft 26.2 (incl. compat/impl/Mc262Compat)
src/mc/26.3/              sources compiled only for Minecraft 26.3 (incl. compat/impl/Mc263Compat)
src/mc/26.4/              sources compiled only for Minecraft 26.4 (incl. compat/impl/Mc264Compat)
                           a version tree is a java source root only; version specific
                           resources live in src/mc/<version>/resources
gradle/mc-versions/       one <version>.properties per supported version,
                          plus an optional <version>.classtweaker access widener
scripts/build-all-versions.sh                builds every registered version
scripts/detect-moved-classes.py              reports which classes moved between two versions
```

`ICompat` must never reference a `net.minecraft.*` type: every method returns primitives, strings,
enums, or version-agnostic types (`McVersion`, `ScreenKind`) plus JOML's `Matrix4f`, which is a
plain library type. The `checkCompatLayer` Gradle task enforces this across all source roots.

## Building a specific version

```
./gradlew build                          # default version (from libs.versions.toml)
./gradlew build -PminecraftVersion=26.3  # explicit version
./scripts/build-all-versions.sh          # one jar per registered version
```

Currently registered: **26.2**, **26.3** and **26.4** (26.4 is a development snapshot, so its
descriptor points at `26.4-snapshot-4`).

Selecting an unknown version fails fast with the list of registered versions.

### What a version descriptor controls

`gradle/mc-versions/<version>.properties` drives everything that differs per version:

| Key | Purpose |
| --- | --- |
| `minecraft`, `fabric-api` | Minecraft and Fabric API coordinates |
| `compat-impl` | FQCN of the `ICompat` implementation substituted into `meteor-compat.properties` |

Alongside the descriptor, `gradle/mc-versions/<version>.classtweaker` is used as the access widener
if it exists, otherwise the shared `src/main/resources/meteor-client.classtweaker`. Wideners drift
too: 26.3 moved the GPU backend classes and made `RenderPipeline.Builder` non-extensible, so the
shared file cannot validate against it.

## Cross-version shims

Most of the code does not need a Minecraft type at all, so it lives in `src/main/java` and is
compiled for every supported version. The few places where a Minecraft type differs between
versions are bridged by two small per-version classes in `src/mc/<version>/.../mc/`:

* **`Vanilla`** — static helpers with the same signature and behaviour in every version, for
  everything that changed shape: `swing(player, hand)`, `copy(blockPos)`, `openUri(url)`,
  `isFuel(item)`, `chat(...)`, `signUpdate(...)`, `mapImage(...)`, `createTexture(...)` and so on.
  The shared code calls `Vanilla.foo(...)` and does not know which Minecraft version it runs on.
* **`MixinTargets`** — the mixin target names that differ. Mixin annotation values have to be
  compile time constants, so the shared mixins refer to `MixinTargets.FOIL_SUBMIT_METHOD` and the
  version specific class holds the actual name.

Both classes are version specific on purpose: they are the only place that knows how 26.2, 26.3
and 26.4 differ. Adding a version means writing those two files, not forking the modules.

What is left in `src/mc/<version>/` is the code that genuinely has to name a Minecraft type:

* the custom renderer, built on `RenderPipeline` and the GPU types (17 files),
* the mixins whose *handler signature* follows a changed method — Mixin requires the callback to
  take exactly the same arguments as the target, so those cannot be shared,
* the mixins that need the target class as a class literal rather than a string,
* the compatibility implementation itself.

Everything else — 918 of the 962 files — is compiled once and shared by all versions.

## How the implementation is selected

`Compat.init()` resolves the implementation class name in this order:

1. the `meteor.compat.impl` system property (handy for tests),
2. the `impl` key of `/meteor-compat.properties` (substituted at build time from the version
   descriptor's `compat-impl`),
3. `meteordevelopment.meteorclient.compat.impl.Mc262Compat` (the default).

`Compat.init()` is called from `MeteorClient.onInitializeClient()` right after the global `mc`
accessor is set, before any `@PreInit` code runs. `Compat.get()` also lazily initializes the layer.

## What is behind the adapter now

These are the operations that changed most often across Minecraft versions:

| Concern | API | Why it is version-fragile |
| --- | --- | --- |
| Version identity | `versionName()`, `version()` | `SharedConstants` / world-version API |
| Window metrics | `getWindowWidth/Height()`, `getGuiScale()` | `Window` accessors |
| Session | `getWorldName()`, `getWorldTime()` | `Minecraft.getCurrentServer`, storage source, `level` |
| Render distance | `getRenderDistance()` | options + private packet-listener field |
| Resource reload | `isLoading()` | private reload-state tracker |
| In-game state | `isInGame()` | `level` / `player` fields |
| HUD visibility | `isHudHidden()`, `setHudHidden()` | deep `GameRenderer` render-state internals |
| Projection | `applyProjection(boolean)` | `Projection`, `ProjectionMatrixBuffer`, `RenderSystem` |
| Screens | `currentScreenKind()`, `closeCurrentScreen()` | `Minecraft.gui` / screen classes get renamed |

Call sites already migrated: `MeteorClient`, `Utils`, `MeteorStarscript`.

## Adding a new Minecraft version

1. Create `gradle/mc-versions/<version>.properties` with the Minecraft version, the matching
   Fabric API version, the compile-only mod versions and `compat-impl`. If the vanilla classpath
   no longer provides a dependency the shared code needs, list it in `extra-compile-deps`.
2. Copy `src/mc/<current>/` to `src/mc/<version>/` and rename the compatibility implementation
   (`Mc262Compat` -> `Mc26xCompat`).
3. Run `scripts/detect-moved-classes.py src/mc/<current> <current> <version>` to get the exact list
   of classes that moved, then rewrite the imports.
4. Compile: `./gradlew build -PminecraftVersion=<version>` and fix what is left by hand.
5. If the access widener no longer validates, add `gradle/mc-versions/<version>.classtweaker`.
6. Fill in `src/mc/<version>/.../mc/Vanilla.java` and `MixinTargets.java` with the new names, and
   move the code that no longer needs a Minecraft type back into `src/main/java`.
7. Audit the mixins (see *Auditing the mixins*): a stale target compiles but crashes the game.

## Third party dependencies

The mod ships with **no third party dependencies**. What is left in the build is:

| Dependency | Kind | Why |
| --- | --- | --- |
| Minecraft, Fabric Loader, Fabric API | platform | the mod runs on it |
| `fabric-renderer-indigo` | compile only, part of Fabric API | the indigo renderer mixin |
| `meteordevelopment:orbit` | bundled (jar-in-jar) | Meteor's own event bus |
| `org.meteordev:starscript` | bundled (jar-in-jar) | the scripting API for addons |

Everything else was removed: the Sodium / Lithium / Iris / ModMenu / ViaFabricPlus / Baritone
integrations, LWJGL's `tinyfd` (files are picked with the in-game `FileBrowserScreen` instead),
`reflections` (addons use `AddonManager#onInitialize` instead of the reflective init), the Netty
SOCKS proxy handlers (the proxy system is gone) and `MinecraftAuth` (the Microsoft account login
is gone).

## Worked example: porting 26.2 to 26.3, and 26.3 to 26.4

Both ports **compile and produce a jar** (`./gradlew build -PminecraftVersion=26.3` /
`=26.4`). See `src/mc/26.3/README.md` and `src/mc/26.4/README.md` for the full status and the
list of runtime risks that still need in-game verification.

### 26.2 -> 26.3

| Stage | Errors |
| --- | --- |
| 26.2 sources compiled against 26.3, untouched | 542 |
| after mechanically rewriting the 27 moved GPU packages | 221 |
| after relocating the 64 affected files into per-version directories | 181 |
| after the render-submission / packet / input rework | 31 |
| after the render-pipeline, GL-mixin and `ItemInHandRenderer` rework | 21 |
| after the authlib 10 port | 9 |
| after the per-version access widener | **0** |

### 26.3 -> 26.4

| Stage | Errors |
| --- | --- |
| 26.3 sources compiled against 26.4, untouched | 100 |
| after the package moves (`RenderPipeline`, `VertexFormat`, `DownloadQueue`) | 24 |
| after the texture rework and the per-version relocations | 1 |
| after the access widener and the `SaveMapCommand` rework | **0** |

26.4 is interesting because Mojang reverted the GPU API experiment of 26.3: the pipeline and
vertex classes are back under `com.mojang.blaze3d.*`, while the rest of `com.mojang.renderpearl.*`
(the backend, the device and texture API) stayed. The texturing stack was reworked on top of that
(`AbstractTexture` -> `TextureHandle`, `DynamicTexture` -> `TextureResources.from2dImage`).

Selected changes, all verified against the 26.3 jar/bytecode before applying:

* All `com.mojang.blaze3d.*` -> `com.mojang.renderpearl.*` import rewrites (27 classes).
* `LivingEntity.swing(hand)` -> `swing(hand, SwingAnimation.DEFAULT, false)`. Disassembling
  26.3's `swing()` shows it only broadcasts `ClientboundSwingAnimationPacket` from a
  `ServerLevel`, so the client no longer sends a swing packet at all.
* `new BlockPos(blockPos)` -> `blockPos.immutable()`; the copy constructor was removed.
* `InputConstants.isKeyDown(window, key)` -> `isKeyDown(key)`.
* `EnderMan` -> `Enderman`.
* **authlib 9 -> 10**: `com.mojang.authlib.yggdrasil.*` -> `com.mojang.authlib.services.*` and
  `YggdrasilAuthenticationService` -> `MinecraftServicesDiscoveryService`.

That is the honest cost of a Minecraft update: the compatibility layer removes the *structural*
churn, but Mojang still changes gameplay and networking APIs that Meteor's ~200 modules use
directly, and those ported call sites change behaviour that cannot be validated by compiling
alone.

### Auditing the mixins

A mixin that targets a method which no longer exists compiles fine and then **throws when the
game starts**, so compiling is not enough. `scripts/audit-mixin-targets.py` checks, for every
mixin:

1. the `@Mixin` target class exists,
2. the mixin has the kind Mixin requires of its target — a class target needs a class mixin, an
   interface target needs an interface mixin (an interface mixin with methods of its own may
   only target an interface, one with only `@Accessor` / `@Invoker` methods may only target a
   class),
3. every `@Shadow` field and method exists on that target class,
4. every injected / overwritten method exists on that target class (walking up the superclass
   chain, because a descriptor names the type the call site used),
5. every `@Accessor` / `@Invoker` member exists on that target class,
6. every `@At(INVOKE)` / `@At(FIELD)` target still resolves **in the bytecode of the target
   method**, with the ordinal it asks for, and the string argument it names is really used,
7. the handler takes the arguments Mixin passes to it — the arguments of the target method and a
   callback, or only the callback — with the same types, and a static method is injected into
   with a static handler.

```
scripts/audit-mixin-targets.py 26.4-snapshot-4 src/mc/26.4 src/main/java
```

The audit runs as a part of `scripts/build-all-versions.sh` and in the CI, so a mixin that does
not fit the version it is built for fails the build instead of the game.

All the checks are clean for 26.2, 26.3 and 26.4. The audit also uncovered mixins that were
already broken — the totem animation, both drop events, the swing duration, the enchant glint,
the block-break particles, and the whole renderer stack of 26.3 and 26.4, where the handlers
still had the arguments of 26.2. Those are fixed for all three versions now.

### Two things the build had to learn

The port also surfaced version differences that a shared build cannot assume. Both are now
modelled per version rather than by hand:

* **Access wideners are version-specific.** `src/main/resources/meteor-client.classtweaker` cannot
  validate against 26.3 (the GPU backend classes moved and `RenderPipeline.Builder` became
  non-extensible). The build prefers `gradle/mc-versions/<version>.classtweaker` and falls back to
  the shared one.
* **The vanilla classpath is not a fixed dependency set.** 26.3 dropped LWJGL's `tinyfd`, which
  Meteor used for file dialogs. Rather than pulling the library back in, the mod now picks files
  with its own in-game `FileBrowserScreen`.


## Migrating existing code

Replace direct Minecraft access with `Compat.get().<method>()`:

```java
// before
public static int getWindowWidth() {
    return mc.getWindow().getWidth();
}

// after
public static int getWindowWidth() {
    return Compat.get().getWindowWidth();
}
```

For `instanceof` checks against Minecraft classes, classify instead:

```java
// before
if (mc.gui.screen() instanceof TitleScreen || mc.gui.screen() instanceof JoinMultiplayerScreen) ...

// after
if (Compat.get().currentScreenKind() == ScreenKind.TITLE) ...
```

## Gotchas

- **Wrapper types.** An operation that must return a Minecraft object needs a version-agnostic
  wrapper (or a classification like `ScreenKind`) before it can cross the adapter boundary.
- **Types Meteor extends.** Meteor's own classes extend Minecraft types (`WidgetScreen extends
  Screen`, `Setting`, ...). Those types necessarily stay in Meteor's signatures.
- **Gameplay and networking APIs are not stable.** Items, packets and entity methods change between
  versions, so those call sites must move either into `src/mc/<version>/` or behind new adapter
  methods.
- **Mixins stay version-specific.** Mixins and accessors target internal signatures by design.

## Verification

```
./gradlew compileJava         # compiles the mod against the configured Minecraft version
./gradlew checkCompatLayer    # asserts the compatibility API stays version-agnostic
./scripts/build-all-versions.sh   # one jar per registered version
```

Compiling is necessary but not sufficient: a mixin whose target method no longer exists still
compiles and then throws while the game starts. The two mixin audits described above cover that
class of failure; what they cannot cover is behaviour, which needs a running game.
