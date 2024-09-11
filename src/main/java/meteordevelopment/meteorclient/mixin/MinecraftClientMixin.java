/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.ItemUseCrosshairTargetEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.game.ResourcePacksReloadedEvent;
import meteordevelopment.meteorclient.events.game.ResolutionChangedEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.mixininterface.IMinecraftClient;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.FastUse;
import meteordevelopment.meteorclient.systems.modules.player.Multitask;
import meteordevelopment.meteorclient.systems.modules.render.UnfocusedCPU;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.CPSUtils;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.network.OnlinePlayers;
import meteordevelopment.starscript.Script;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.concurrent.CompletableFuture;

@Mixin(value = MinecraftClient.class, priority = 1001)
public abstract class MinecraftClientMixin implements IMinecraftClient {
    @Unique private boolean doItemUseCalled;
    @Unique private boolean rightClick;
    @Unique private long lastTime;
    @Unique private boolean firstFrame;

    @Shadow public ClientWorld world;
    @Shadow @Final public Mouse mouse;
    @Shadow @Final private Window window;
    @Shadow public Screen currentScreen;
    @Shadow @Final public GameOptions options;

    @Shadow protected abstract void doItemUse();
    @Shadow public abstract Profiler getProfiler();
    @Shadow public abstract boolean isWindowFocused();

    @Shadow
    @Nullable
    public ClientPlayerInteractionManager interactionManager;

    @Shadow
    private int itemUseCooldown;

    @Shadow
    @Nullable
    public ClientPlayerEntity player;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        MeteorClient.INSTANCE.onInitializeClient();
        firstFrame = true;
    }

    @Inject(at = @At("HEAD"), method = "tick")
    private void onPreTick(CallbackInfo info) {
        OnlinePlayers.update();

        doItemUseCalled = false;

        getProfiler().push(MeteorClient.MOD_ID + "_pre_update");
        MeteorClient.EVENT_BUS.post(TickEvent.Pre.get());
        getProfiler().pop();

        if (rightClick && !doItemUseCalled && interactionManager != null) doItemUse();
        rightClick = false;
    }

    @Inject(at = @At("TAIL"), method = "tick")
    private void onTick(CallbackInfo info) {
        getProfiler().push(MeteorClient.MOD_ID + "_post_update");
        MeteorClient.EVENT_BUS.post(TickEvent.Post.get());
        getProfiler().pop();
    }

    @Inject(method = "doAttack", at = @At("HEAD"))
    private void onAttack(CallbackInfoReturnable<Boolean> cir) {
        CPSUtils.onAttack();
    }

    @Inject(method = "doItemUse", at = @At("HEAD"))
    private void onDoItemUse(CallbackInfo info) {
        doItemUseCalled = true;
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;Z)V", at = @At("HEAD"))
    private void onDisconnect(Screen screen, boolean transferring, CallbackInfo info) {
        if (world != null) {
            MeteorClient.EVENT_BUS.post(GameLeftEvent.get());
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo info) {
        if (screen instanceof WidgetScreen) screen.mouseMoved(mouse.getX() * window.getScaleFactor(), mouse.getY() * window.getScaleFactor());

        OpenScreenEvent event = OpenScreenEvent.get(screen);
        MeteorClient.EVENT_BUS.post(event);

        if (event.isCancelled()) info.cancel();
    }

    @Inject(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isItemEnabled(Lnet/minecraft/resource/featuretoggle/FeatureSet;)Z"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void onDoItemUseHand(CallbackInfo ci, Hand[] var1, int var2, int var3, Hand hand, ItemStack itemStack) {
        FastUse fastUse = Modules.get().get(FastUse.class);
        if (fastUse.isActive()) {
            itemUseCooldown = fastUse.getItemUseCooldown(itemStack);
        }
    }

    @ModifyExpressionValue(method = "doItemUse", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;crosshairTarget:Lnet/minecraft/util/hit/HitResult;", ordinal = 1))
    private HitResult doItemUseMinecraftClientCrosshairTargetProxy(HitResult original) {
        return MeteorClient.EVENT_BUS.post(ItemUseCrosshairTargetEvent.get(original)).target;
    }

    @ModifyReturnValue(method = "reloadResources(ZLnet/minecraft/client/MinecraftClient$LoadingContext;)Ljava/util/concurrent/CompletableFuture;", at = @At("RETURN"))
    private CompletableFuture<Void> onReloadResourcesNewCompletableFuture(CompletableFuture<Void> original) {
        return original.thenRun(() -> MeteorClient.EVENT_BUS.post(ResourcePacksReloadedEvent.get()));
    }

    @ModifyArg(method = "updateWindowTitle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;setTitle(Ljava/lang/String;)V"))
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

    @Inject(method = "onResolutionChanged", at = @At("TAIL"))
    private void onResolutionChanged(CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(ResolutionChangedEvent.get());
    }

    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void onGetFramerateLimit(CallbackInfoReturnable<Integer> info) {
        if (Modules.get().isActive(UnfocusedCPU.class) && !isWindowFocused()) info.setReturnValue(Math.min(Modules.get().get(UnfocusedCPU.class).fps.get(), this.options.getMaxFps().getValue()));
    }

    // Time delta

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(CallbackInfo info) {
        long time = System.currentTimeMillis();

        if (firstFrame) {
            lastTime = time;
            firstFrame = false;
        }

        Utils.frameTime = (time - lastTime) / 1000.0;
        lastTime = time;
    }

    // multitask

    @ModifyExpressionValue(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;isBreakingBlock()Z"))
    private boolean doItemUseModifyIsBreakingBlock(boolean original) {
        return !Modules.get().isActive(Multitask.class) && original;
    }

    @ModifyExpressionValue(method = "handleBlockBreaking", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    private boolean handleBlockBreakingModifyIsUsingItem(boolean original) {
        return !Modules.get().isActive(Multitask.class) && original;
    }

    @ModifyExpressionValue(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z", ordinal = 0))
    private boolean handleInputEventsModifyIsUsingItem(boolean original) {
        return !Modules.get().get(Multitask.class).attackingEntities() && original;
    }

    @Inject(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z", ordinal = 0, shift = At.Shift.BEFORE))
    private void handleInputEventsInjectStopUsingItem(CallbackInfo info) {
        if (Modules.get().get(Multitask.class).attackingEntities() && player.isUsingItem()) {
            if (!options.useKey.isPressed()) interactionManager.stopUsingItem(player);
            while (options.useKey.wasPressed());
        }
    }

    // Interface

    @Override
    public void meteor_client$rightClick() {
        rightClick = true;
    }
}
