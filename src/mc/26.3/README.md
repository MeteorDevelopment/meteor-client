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
| after adding the per-version access widener | **0** |

## Build-system changes needed for 26.3

Two things turned out to be version-specific that the shared build did not model yet:

* **Access widener.** 26.3 moved the GPU backend classes, and `RenderPipeline.Builder` is no longer
  extensible, so `src/main/resources/meteor-client.classtweaker` no longer validates. The build now
  prefers `gradle/mc-versions/<version>.classtweaker` and falls back to the shared one. The 26.3
  variant is `gradle/mc-versions/26.3.classtweaker`.

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

Some mixins could not be shared, because Mixin requires the handler to take exactly the
arguments of the method it injects into, and those methods changed:

| Mixin | 26.2 | 26.3 / 26.4 |
| --- | --- | --- |
| `EntityShouldRenderMixin` | `EntityRenderer#shouldRender(T, Frustum, double, double, double)` | `... , float` |
| `GameRendererRenderLevelMixin` | `GameRenderer#renderLevel(DeltaTracker)` | `GameRenderer#renderLevel()` |
| `LightningBoltRendererMixin` | `LightningBoltRenderer#quad(..., float, float, float, ...)` | the three colour arguments are gone |

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
**six mixins that were broken in 26.3** (the totem animation, both drop events, the swing
duration, the enchant glint and the block-break particles) and **the whole renderer stack of 26.3
and 26.4**, whose handlers still had the arguments of 26.2 — the game could not even start there.
Those are fixed now for all three versions.

## Conventions

* Files touching changed APIs are duplicated per version, so the shared code in
  `src/main/java` stays version-agnostic.
* Changes here are *not* applied to `src/mc/26.2/`. Fix both trees when a fix belongs to both.
