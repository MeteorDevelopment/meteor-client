/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.DoAttackEvent;
import meteordevelopment.meteorclient.events.entity.player.DoItemUseEvent;
import meteordevelopment.meteorclient.events.entity.player.ItemUseCrosshairTargetEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.game.ResolutionChangedEvent;
import meteordevelopment.meteorclient.events.game.ResourcePacksReloadedEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.mixininterface.IMinecraftClient;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.InventoryTweaks;
import meteordevelopment.meteorclient.systems.modules.movement.GUIMove;
import meteordevelopment.meteorclient.systems.modules.player.FastUse;
import meteordevelopment.meteorclient.systems.modules.player.Multitask;
import meteordevelopment.meteorclient.systems.modules.render.ESP;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.world.HighwayBuilder;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.CPSUtils;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.network.OnlinePlayers;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.meteordev.starscript.Script;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(value = Minecraft.class, priority = 1001)
public abstract class MinecraftClientMixin implements IMinecraftClient {
    @Unique private boolean doItemUseCalled;
    @Unique private boolean rightClick;
    @Unique private long lastTime;
    @Unique private boolean firstFrame;

    @Shadow public ClientLevel level;
    @Shadow @Final public MouseHandler mouseHandler;
    @Shadow @Final private Window window;
    @Shadow public Screen screen;
    @Shadow @Final public Options options;

    @Shadow protected abstract void startUseItem();

    @Shadow
    @Nullable
    public MultiPlayerGameMode gameMode;

    @Shadow
    private int rightClickDelay;

    @Shadow
    @Nullable
    public LocalPlayer player;

    @Shadow
    public HitResult hitResult;

    @Shadow
    public Entity crosshairPickEntity;

    @Shadow
    @Final
    @Mutable
    private RenderTarget mainRenderTarget;

    @Shadow
    protected abstract void continueAttack(boolean breaking);

    @Shadow
    public abstract Entity getCameraEntity();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        MeteorClient.INSTANCE.onInitializeClient();
        firstFrame = true;
    }

    @Inject(at = @At("HEAD"), method = "tick")
    private void onPreTick(CallbackInfo info) {
        OnlinePlayers.update();

        doItemUseCalled = false;

        Profiler.get().push(MeteorClient.MOD_ID + "_pre_update");
        MeteorClient.EVENT_BUS.post(TickEvent.Pre.get());
        Profiler.get().pop();

        if (rightClick && !doItemUseCalled && gameMode != null) startUseItem();
        rightClick = false;
    }

    @Inject(at = @At("TAIL"), method = "tick")
    private void onTick(CallbackInfo info) {
        Profiler.get().push(MeteorClient.MOD_ID + "_post_update");
        MeteorClient.EVENT_BUS.post(TickEvent.Post.get());
        Profiler.get().pop();
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void onAttack(CallbackInfoReturnable<Boolean> cir) {
        CPSUtils.onAttack();
        if (MeteorClient.EVENT_BUS.post(DoAttackEvent.get()).isCancelled()) cir.cancel();
    }

    // Freecam / HighwayBuilder pick path moved from GameRenderer to Minecraft in 26.1.
    @Unique private boolean freecamSet;

    @Inject(method = "pick", at = @At("HEAD"), cancellable = true)
    private void onPick(float tickDelta, CallbackInfo ci) {
        Freecam freecam = Modules.get().get(Freecam.class);
        boolean highwayBuilder = Modules.get().isActive(HighwayBuilder.class);
        Entity cameraE = getCameraEntity();

        if (!(freecam.isActive() || highwayBuilder) || cameraE == null || freecamSet || level == null || player == null) return;

        ci.cancel();
        Profiler.get().push("pick");

        double x = cameraE.getX();
        double y = cameraE.getY();
        double z = cameraE.getZ();
        double lastX = cameraE.xo;
        double lastY = cameraE.yo;
        double lastZ = cameraE.zo;
        float yaw = cameraE.getYRot();
        float pitch = cameraE.getXRot();
        float lastYaw = cameraE.yRotO;
        float lastPitch = cameraE.xRotO;

        try {
            if (highwayBuilder) {
                cameraE.setYRot(MeteorClient.mc.gameRenderer.getMainCamera().yRot());
                cameraE.setXRot(MeteorClient.mc.gameRenderer.getMainCamera().xRot());
            } else {
                ((IVec3d) cameraE.position()).meteor$set(freecam.pos.x, freecam.pos.y - cameraE.getEyeHeight(cameraE.getPose()), freecam.pos.z);
                cameraE.xo = freecam.prevPos.x;
                cameraE.yo = freecam.prevPos.y - cameraE.getEyeHeight(cameraE.getPose());
                cameraE.zo = freecam.prevPos.z;
                cameraE.setYRot(freecam.yaw);
                cameraE.setXRot(freecam.pitch);
                cameraE.yRotO = freecam.lastYaw;
                cameraE.xRotO = freecam.lastPitch;
            }

            freecamSet = true;
            hitResult = player.raycastHitResult(tickDelta, cameraE);
            crosshairPickEntity = hitResult instanceof EntityHitResult entityHitResult ? entityHitResult.getEntity() : null;
        } finally {
            freecamSet = false;

            ((IVec3d) cameraE.position()).meteor$set(x, y, z);
            cameraE.xo = lastX;
            cameraE.yo = lastY;
            cameraE.zo = lastZ;
            cameraE.setYRot(yaw);
            cameraE.setXRot(pitch);
            cameraE.yRotO = lastYaw;
            cameraE.xRotO = lastPitch;

            Profiler.get().pop();
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"))
    private void onDoItemUse(CallbackInfo info) {
        doItemUseCalled = true;
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("HEAD"))
    private void onDisconnect(Screen screen, boolean transferring, boolean stopSound, CallbackInfo info) {
        if (level != null) {
            MeteorClient.EVENT_BUS.post(GameLeftEvent.get());
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo info) {
        if (screen instanceof WidgetScreen) screen.mouseMoved(mouseHandler.xpos() * window.getGuiScale(), mouseHandler.ypos() * window.getGuiScale());

        OpenScreenEvent event = OpenScreenEvent.get(screen);
        MeteorClient.EVENT_BUS.post(event);

        if (event.isCancelled()) info.cancel();
    }

    @WrapOperation(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;releaseAll()V"))
    private void onSetScreenKeyBindingUnpressAll(Operation<Void> op) {
        Modules modules = Modules.get();
        if (modules == null) {
            op.call();
            return;
        }

        GUIMove guimove = modules.get(GUIMove.class);
        if (guimove == null || !guimove.isActive() || guimove.skip()) {
            op.call();
            return;
        }

        Options options = MeteorClient.mc.options;
        for (KeyMapping kb : KeyBindingAccessor.getKeysById().values()) {
            if (kb == options.keyUp) continue;
            if (kb == options.keyLeft) continue;
            if (kb == options.keyRight) continue;
            if (kb == options.keyDown) continue;
            if (guimove.sneak.get() && kb == options.keyShift) continue;
            if (guimove.sprint.get() && kb == options.keySprint) continue;
            if (guimove.jump.get() && kb == options.keyJump) continue;
            ((KeyBindingAccessor) kb).meteor$invokeReset();
        }
    }

    @Inject(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isItemEnabled(Lnet/minecraft/world/flag/FeatureFlagSet;)Z"))
    private void onDoItemUseHand(CallbackInfo ci, @Local ItemStack itemStack) {
        FastUse fastUse = Modules.get().get(FastUse.class);
        if (fastUse.isActive()) {
            rightClickDelay = fastUse.getItemUseCooldown(itemStack);
        }
    }

    @Inject(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/InteractionHand;values()[Lnet/minecraft/world/InteractionHand;"), cancellable = true)
    private void onDoItemUseBeforeHands(CallbackInfo ci) {
        if (MeteorClient.EVENT_BUS.post(DoItemUseEvent.get()).isCancelled()) ci.cancel();
    }

    @ModifyExpressionValue(method = "startUseItem", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;hitResult:Lnet/minecraft/world/phys/HitResult;", ordinal = 1))
    private HitResult doItemUseMinecraftClientCrosshairTargetProxy(HitResult original) {
        return MeteorClient.EVENT_BUS.post(ItemUseCrosshairTargetEvent.get(original)).target;
    }

    @ModifyReturnValue(method = "reloadResourcePacks(ZLnet/minecraft/client/Minecraft$GameLoadCookie;)Ljava/util/concurrent/CompletableFuture;", at = @At("RETURN"))
    private CompletableFuture<Void> onReloadResourcesNewCompletableFuture(CompletableFuture<Void> original) {
        return original.thenRun(() -> MeteorClient.EVENT_BUS.post(ResourcePacksReloadedEvent.get()));
    }

    @ModifyArg(method = "updateTitle", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;setTitle(Ljava/lang/String;)V"))
    private String setTitle(String original) {
        if (Config.get() == null || !Config.get().customWindowTitle.get()) return original;

        String customTitle = Config.get().customWindowTitleText.get();
        Script script = MeteorStarscript.compile(customTitle);

        if (script != null) {
            String title = MeteorStarscript.run(script);
            if (title != null) customTitle = title;
        }

        return customTitle;
    }

    // Have to add this condition if we want to draw back a bow using packets, without it getting cancelled by vanilla code
    @WrapWithCondition(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;releaseUsingItem(Lnet/minecraft/world/entity/player/Player;)V"))
    private boolean wrapStopUsing(MultiPlayerGameMode instance, Player player) {
        return HB$stopUsingItem();
    }

    @Unique
    private boolean HB$stopUsingItem() {
        HighwayBuilder b = Modules.get().get(HighwayBuilder.class);
        return !b.isActive() || !b.drawingBow;
    }

    @Inject(method = "resizeGui", at = @At("TAIL"))
    private void onResolutionChanged(CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(ResolutionChangedEvent.get());
    }

    // Time delta

    @Inject(method = "runTick", at = @At("HEAD"))
    private void onRender(CallbackInfo info) {
        long time = System.currentTimeMillis();

        if (firstFrame) {
            lastTime = time;
            firstFrame = false;
        }

        Utils.frameTime = (time - lastTime) / 1000.0;
        lastTime = time;
    }

    // Multitask

    @ModifyExpressionValue(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;isDestroying()Z"))
    private boolean doItemUseModifyIsBreakingBlock(boolean original) {
        return !Modules.get().isActive(Multitask.class) && original;
    }

    @ModifyExpressionValue(method = "continueAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean handleBlockBreakingModifyIsUsingItem(boolean original) {
        return !Modules.get().isActive(Multitask.class) && original;
    }

    @ModifyExpressionValue(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z", ordinal = 0))
    private boolean handleInputEventsModifyIsUsingItem(boolean original) {
        return !Modules.get().get(Multitask.class).attackingEntities() && original;
    }

    @Inject(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z", ordinal = 0, shift = At.Shift.BEFORE))
    private void handleInputEventsInjectStopUsingItem(CallbackInfo info) {
        if (Modules.get().get(Multitask.class).attackingEntities() && player.isUsingItem()) {
            if (!options.keyUse.isDown() && HB$stopUsingItem()) gameMode.releaseUsingItem(player);
            //noinspection StatementWithEmptyBody
            while (options.keyUse.consumeClick());
        }
    }

    // Glow esp

    @ModifyReturnValue(method = "shouldEntityAppearGlowing", at = @At("RETURN"))
    private boolean hasOutlineModifyIsOutline(boolean original, Entity entity) {
        ESP esp = Modules.get().get(ESP.class);
        if (esp == null) return original;
        if (!esp.isGlow() || esp.shouldSkip(entity)) return original;

        return esp.getColor(entity) != null || original;
    }


    // faster inputs

    @Unique
    private boolean isBreaking = false;

    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;handleKeybinds()V"))
    private boolean wrapHandleInputEvents(Minecraft instance) {
        return !Modules.get().get(InventoryTweaks.class).frameInput();
    }

    @WrapWithCondition(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;continueAttack(Z)V"))
    private boolean wrapHandleBlockBreaking(Minecraft instance, boolean breaking) {
        isBreaking = breaking;
        return !Modules.get().get(InventoryTweaks.class).frameInput();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;handleKeybinds()V", shift = At.Shift.AFTER))
    private void afterHandleInputEvents(CallbackInfo ci) {
        if (!Modules.get().get(InventoryTweaks.class).frameInput()) return;

        continueAttack(isBreaking);
        isBreaking = false;
    }

    // Interface

    @Override
    public void meteor$rightClick() {
        rightClick = true;
    }

    @Override
    public void meteor$setFramebuffer(RenderTarget framebuffer) {
        this.mainRenderTarget = framebuffer;
    }
}
