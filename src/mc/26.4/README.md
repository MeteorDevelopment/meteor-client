# Minecraft 26.4 sources

This directory holds the Minecraft 26.4 variant of the version-specific sources.

## Status

`./gradlew build -PminecraftVersion=26.4` **succeeds** and produces
`build/libs/meteor-client-26.4-snapshot-4-local.jar`. `checkCompatLayer` and
`validateAccessWidener` pass, and the mixin audits (see below) are clean.

26.4 is still a development snapshot, so the Minecraft coordinate points at
`26.4-snapshot-4` — the newest snapshot Fabric API covers (`0.164.0+26.4`).

### Error history

Measured with `./gradlew compileJava -PminecraftVersion=26.4` (distinct error sites):

| Stage | Errors |
| --- | --- |
| 26.3 sources compiled against 26.4, untouched | 100 |
| after the package moves (`RenderPipeline`, `VertexFormat`, `DownloadQueue`) | 24 |
| after the texture rework and the per-version relocations | 1 |
| after the access widener and the `SaveMapCommand` rework | **0** |

## What changed in 26.4

The interesting part of 26.4 is that Mojang **reverted the GPU API experiment**: the
`com.mojang.renderpearl.*` packages that 26.3 introduced are back under `com.mojang.blaze3d.*`
for the pipeline and vertex classes, while the rest of `renderpearl` (the backend, the device and
texture API) stayed.

| Change | Notes |
| --- | --- |
| `RenderPipeline` | `com.mojang.renderpearl.api.pipeline` -> `com.mojang.blaze3d.pipeline` |
| `VertexFormat`, `VertexFormatElement` | `com.mojang.renderpearl.api.vertex` -> `com.mojang.blaze3d.vertex` |
| `AbstractTexture` | Replaced by `TextureHandle` (`textureView()` / `sampler()`); `DynamicTexture` is gone |
| Textures | `TextureManager#getTexture` now returns a `TextureHandle`; new textures are created with `TextureResources.from2dImage(...)` |
| `Mth.clamp` | Removed. The shared code now uses `meteordevelopment.meteorclient.utils.misc.MathUtils#clamp`, which keeps the vanilla semantics (the JDK's `Math.clamp` throws when `min > max`) |
| `Button.Builder` constructor | Now `protected`; the shared code uses the `Button.builder(...)` factory, which exists in every supported version |
| `AbstractWidget#visible` | Now `protected`; accessed through the new `AbstractWidgetAccessor` |
| `Fluid` | `Fluids.FLOWING_WATER` / `FLOWING_LAVA` merged into `Fluids.WATER` / `Fluids.LAVA`; `Fluid#isSame` removed |
| `Level` / `ClientLevel` constructors | Lost the `long` (biome zoom seed) and the sea level parameter |
| `Camera#getFluidInCamera` | Renamed to `Camera#getFogType` |
| `BuiltInRegistries#internalRegister` | Moved to `BuiltinRegistryBuilder` |
| `ShaderManager#apply` | New signature: `apply(PipelineBuilder, ShaderManager$PendingResults)` |
| `LocalPlayer#displayItemActivation` | The totem animation moved from `GameRenderer` to `LocalPlayer` |
| `LivingEntity#drop` | Now `drop(ItemStack, boolean, Prediction)` on `LivingEntity` instead of `Player` |
| `MultiPlayerGameMode#dropItem` | The "drop hotbar" (Q) action moved off `LocalPlayer#drop` |
| `LivingEntity#getCurrentSwingDuration` | Renamed to `getModifiedSwingDuration` |
| `ItemFeatureRenderer#prepareFoilSubmit` | Renamed to `prepareSubmit` |
| `ClientLevel#addBreakingBlockEffect` | Renamed to `addBreakingBlockEffects` and gained a `boolean` |
| `MapTextureManager$MapInstance#texture` | Now a `GpuTexture` that cannot be read back; the 26.4 `SaveMapCommand` rebuilds the image from the saved data instead |

## Cross-version refactor

After the port compiled, most of the duplicated code was moved back into the shared tree
(`src/main/java`), behind two small per-version classes:

* `meteordevelopment.meteorclient.mc.Vanilla` — the version specific shims (`swing`, `copy`,
  `openUri`, `isFuel`, `chat`, `signUpdate`, `mapImage`, `createTexture`, `blocksMotion`, ...).
* `meteordevelopment.meteorclient.mc.MixinTargets` — the mixin target names that differ.

| | before | after |
| --- | --- | --- |
| `src/main/java` | 837 files | 918 files |
| `src/mc/<version>/` | 122 files | 44 files |

234 files of duplication are gone; the shared tree is now 98 393 lines against ~4 950 lines per
version.

What is still version specific is the code that has to *name* a Minecraft type:

* the custom renderer (`RenderPipeline`, the GPU types) — 17 files,
* the mixins whose handler signature follows a changed method — Mixin requires the callback to
  have exactly the same arguments as the target, so `ItemInHandRendererMixin`, the three entity
  renderer mixins, `ItemDropMixin` and `DropAllMixin` cannot be shared,
* the mixins that target a class that moved to another package and need it as a class literal,
* the compatibility implementation itself.

The shims grew along with this: `Vanilla` now covers the swing, the textures, the tooltips, the
banners, the keyboard, the auth services and the map images, and `MixinTargets` holds the
differing target names.

## Files that had to become version specific

These were shared in `src/main` and are now duplicated per version (the FQN is unchanged, so the
calling code did not have to change):

`Waypoints`, `Waypoint`, `Capes`, `FakeClientPlayer`, `SaveMapCommand`, `ClientLevelMixin`,
`CameraMixin`, `BuiltInRegistriesMixin`, `ShaderManagerMixin`, `DownloadQueueMixin`,
`MapTextureManagerAccessor`, `ItemRendererMixin`, `SodiumDefaultFluidRendererMixin`,
`LightningBoltRendererMixin`, plus the new `ItemActivationMixin`, `ItemDropMixin`, `DropAllMixin`,
`SwingDurationMixin`, `EntityShouldRenderMixin` and `GameRendererRenderLevelMixin`.

The last two are new because Mixin requires the handler to take exactly the arguments of the
method it injects into, and those methods changed:

| Mixin | 26.2 | 26.3 / 26.4 |
| --- | --- | --- |
| `EntityShouldRenderMixin` | `EntityRenderer#shouldRender(T, Frustum, double, double, double)` | `... , float` |
| `GameRendererRenderLevelMixin` | `GameRenderer#renderLevel(DeltaTracker)` | `GameRenderer#renderLevel()` |

`CloudRendererMixin`, `ScreenEffectRendererMixin` and the `renderItemInHand` injection of
`GameRendererMixin` went the other way: their handlers did not use the arguments at all, so they
take only the callback now and are shared by every version again.

## Mixin audits

`scripts/audit-mixin-targets.py` catches the mixins that compile but cannot be applied at runtime
(a missing method makes Mixin throw when the game starts). It checks that the target class
exists, that the mixin has the kind Mixin requires of its target, that the `@Shadow`,
`@Accessor` and `@Invoker` members exist, that the `@At(INVOKE)` / `@At(FIELD)` targets are really
in the bytecode of the target method, and that the handler takes the arguments Mixin passes to it.

The audit is clean for 26.2, 26.3 and 26.4, and it runs as a part of the build. It uncovered
**six mixins that were already broken in 26.3** (a totem animation, the drop events, the swing
duration, the enchant glint and the block-break particles) and **the whole renderer stack of 26.3
and 26.4**, whose handlers still had the arguments of 26.2 — the game could not even start there.
Those are fixed now for all three versions.

## Runtime risks

Compiling is not the same as working. This environment has no game and no server, so **none** of
the behaviour below has been exercised.

| Item | Risk | Notes |
| --- | --- | --- |
| Sodium / Lithium / Iris | Not supported | There is no build of these mods for 26.4 yet, and their 26.3 builds are compiled against APIs that 26.4 removed. The mixin plugin only applies those mixins when the mod is loaded, so they stay dormant. |
| Item in hand rendering | High | `FirstPersonHandsAndItemsRenderer` was reworked for 26.3 and reworked again for 26.4. The injection points were re-derived from the bytecode but never executed. |
| `HandView` equip animations | Feature lost | 26.3 removed `tick()` / `shouldInstantlyReplaceVisibleItem()` / the renderer's `mainHandHeight` fields, so HandView's "skip swapping" and "old animations" options have no hook point. |
| Fog rework | Medium | 26.4 replaced the fog type with a `FogEnvironment` system; `NoRender`'s "no liquid overlay" now hooks `Camera#getFogType` instead of `Camera#getFluidInCamera`. |
| Map saving | Medium | The 26.4 `SaveMapCommand` rebuilds the image from `MapItemSavedData#colors` instead of reading the GPU texture back. The pixels are identical to what `MapTextureManager` uploads, but the path is different. |
| `Mth.clamp` replacement | Low | `MathUtils.clamp` keeps the vanilla semantics, including the "min > max" case that the JDK's `Math.clamp` rejects. |
