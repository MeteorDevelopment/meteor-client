# Minecraft 26.3 sources

This directory holds the Minecraft 26.3 variant of the version-specific sources.

## Status

`./gradlew build -PminecraftVersion=26.3` **succeeds** and produces
`build/libs/meteor-client-26.3-local.jar`. `checkCompatLayer` and `validateAccessWidener` pass.

That means the port compiles. It does **not** mean it is correct — see *Runtime risks* below.

### Error history

Measured with `./gradlew compileJava -PminecraftVersion=26.3` (distinct error sites):

| Stage | Errors |
| --- | --- |
| 26.2 sources compiled against 26.3, untouched | 542 |
| after mechanically rewriting the 27 moved GPU packages | 221 |
| after relocating the 64 affected files into per-version directories | 181 |
| after the render-submission / packet / input rework | 31 |
| after the mechanical API batch (`drop`, chat, banner, `MaterialInfo`, ...) | 29 |
| after the render-pipeline + GL-mixin + `ItemInHandRenderer` rework | 21 |
| after the authlib 10 port | 9 |
| after adding the per-version access widener + tinyfd dependency | **0** |

## Build-system changes needed for 26.3

Two things turned out to be version-specific that the shared build did not model yet:

* **Access widener.** 26.3 moved the GPU backend classes, and `RenderPipeline.Builder` is no longer
  extensible, so `src/main/resources/meteor-client.classtweaker` no longer validates. The build now
  prefers `gradle/mc-versions/<version>.classtweaker` and falls back to the shared one. The 26.3
  variant is `gradle/mc-versions/26.3.classtweaker`.
* **Dependencies the vanilla classpath no longer provides.** Minecraft 26.3 dropped LWJGL's
  `tinyfd`, which Meteor's shared code uses for file dialogs. The version descriptor can now
  declare `extra-compile-deps=group:artifact:version`, and 26.3 declares
  `org.lwjgl:lwjgl-tinyfd:3.4.1`. See *Runtime risks* — this only makes it compile.

## Done

Verified against the 26.3 jar/bytecode before applying:

* All `com.mojang.blaze3d.*` → `com.mojang.renderpearl.*` import rewrites (27 classes).
* `LivingEntity.swing(hand)` → `swing(hand, SwingAnimation.DEFAULT, false)`. `swing()` only
  broadcasts `ClientboundSwingAnimationPacket` from a `ServerLevel`, so the client no longer
  sends a swing packet at all.
* `new BlockPos(blockPos)` → `blockPos.immutable()`.
* `InputConstants.isKeyDown(window, key)` → `isKeyDown(key)`.
* `EnderMan` → `Enderman`.
* LWJGL/GLFW → SDL input migration (`KeyboardHandlerMixin`, key constants, `Keybind`).
* `ServerboundSwingPacket` send sites → `swing()`; `ServerboundChatPacket` gained an
  `Optional<MessageSignature>`.
* `DynamicUniformStorage` → `DynamicUniformStorageMapped`.
* Render submission rework: `AvatarRendererMixin`, `LivingEntityRendererMixin`,
  `EndCrystalRendererMixin`, `BannerRendererMixin`, `OutlineRenderCommandQueue`,
  `SimpleBlockRenderer`.
* `RenderPass.withSampler(...)` → `RenderPass.setUniform(...)`;
  `RenderSystem.getCompiledPipeline(RenderPipeline)`.
* `RenderPipeline.Builder` has a **private constructor** in 26.3, so
  `ExtendedRenderPipelineBuilder` now *wraps* `RenderPipeline.builder(...)` instead of extending it.
* `com.mojang.renderpearl.backend.opengl.{GlDevice,GlCommandEncoder}` became package-private, so
  `GlDeviceMixin` / `GlCommandEncoderMixin` use `@Mixin(targets = "...")`. The command encoder's
  `device` field is shadowed as its public supertype `GpuDeviceBackend`.
* `FeatureRenderDispatcher$PreparedFrame.executeOutline()` now takes a `RenderPass`, so
  `LevelRendererMixin` creates the pass itself.
* `ItemInHandRenderer` → `FirstPersonHandsAndItemsRenderer`; hand items and the swing animation
  moved into `FirstPersonHandsAndItemsRenderState` / `AvatarRenderState.swingAnimation`.
* `ItemStackRenderState.LayerRenderState.prepareQuadList()` → the `quads` field is now a private
  `ItemQuads` (no getter), exposed through a new `@Accessor` on `LayerRenderStateAccessor`.
* `VertexConsumer.setUv3(float, float)` added in 26.3 — implemented in the custom vertex consumers.
* `SubmitNodeCollector.submitModel(...)` now takes a `UvMapping` instead of a
  `TextureAtlasSprite`, and dropped `CrumblingOverlay`.
* `BakedQuad.MaterialInfo` grew to 8 arguments; `BannerRenderer.submitPatterns` dropped
  `CrumblingOverlay`; `SkyRenderState.skyColor` became a `Vector3fc`.
* `LocalPlayer.drop(...)` → `LivingEntity.drop(ItemStack, boolean, Prediction)`;
  `BlockStateBase.blocksMotion()` → `isSolid()`; `BedRule.explodes()` → `destroyOnUse()`.
* `Item.components()` replaces `builtInRegistryHolder()` for component access.
* **authlib 9 → 10** (10.0.77): `com.mojang.authlib.yggdrasil.*` → `com.mojang.authlib.services.*`,
  and `YggdrasilAuthenticationService` was replaced by `MinecraftServicesDiscoveryService`
  (`create(Proxy, boolean[, Environment])`). `Account`, `MinecraftAccessor` and
  `TheAlteningAccount` are ported to it.

## Runtime risks

Compiling is not the same as working. This environment has no game and no server, so **none** of the
behavioural changes below have been exercised. Do not ship a 26.3 jar before testing them.

| Item | Risk | Notes |
| --- | --- | --- |
| `tinyfd` file dialogs | `NoClassDefFoundError` | 26.3 no longer ships `lwjgl-tinyfd`, and the build only adds it as `compileOnly`. The jar (and its natives) must be bundled for Save/Load dialogs to work. |
| Item in hand rendering | High | `FirstPersonHandsAndItemsRenderer` was rewritten, not renamed. Injection points were re-derived from the bytecode but never executed. |
| `HandView` equip animations | Feature lost | 26.3 removed `tick()` / `shouldInstantlyReplaceVisibleItem()` / the renderer's `mainHandHeight` fields, so HandView's "skip swapping" and "old animations" options have no hook point. Needs a new design. |
| The Altening login | Medium | `authlib` 10 derives endpoints from a discovery URL instead of three host names; the port feeds it the old auth server host. |
| Swing packets, eat transform, hand events | Medium | All reworked (`@ModifyExpressionValue` on `AvatarRenderState.swingAnimation`, injectors on `submitArmWithItem`/`applyEatTransform`). |
| Sodium / Lithium / Iris | Not supported | There is no build of these mods for 26.3 that matches the new GPU API, and their mixins are compiled against APIs that changed. The mixin plugin only applies them when the mod is loaded. |

The `ShaderManagerMixin` that used to be stale for 26.3 has been fixed: the mixin is now
version specific and targets `apply(GpuDevice, ShaderManager$PendingResults)`.

## Cross-version refactor

Most of the code that used to be duplicated per version was moved back into the shared tree
(`src/main/java`) behind two small per-version classes: `meteordevelopment.meteorclient.mc.Vanilla`
(the version specific shims) and `meteordevelopment.meteorclient.mc.MixinTargets` (the mixin target
names that differ). See `src/mc/26.4/README.md` for the details and the table.

## Mixin audits

Two audits are run over the mixins to catch targets that compile but cannot be applied at runtime
(a missing method makes Mixin throw when the game starts):

* every injected method exists on its target class,
* every `@At(INVOKE)` / `@At(FIELD)` target still resolves.

Both are clean for 26.2, 26.3 and 26.4. They also uncovered six mixins that were broken in 26.3
(the totem animation, both drop events, the swing duration, the enchant glint and the block-break
particles); those are fixed for all three versions now.

## Conventions

* Files touching changed APIs are duplicated per version, so the shared code in
  `src/main/java` stays version-agnostic.
* Changes here are *not* applied to `src/mc/26.2/`. Fix both trees when a fix belongs to both.
