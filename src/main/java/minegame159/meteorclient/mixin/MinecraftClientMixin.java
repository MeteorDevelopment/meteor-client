/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.game.GameLeftEvent;
import minegame159.meteorclient.events.game.OpenScreenEvent;
import minegame159.meteorclient.events.game.ResourcePacksReloadedEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.gui.GuiKeyEvents;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.mixininterface.IMinecraftClient;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.player.AutoEat;
import minegame159.meteorclient.modules.player.AutoGap;
import minegame159.meteorclient.utils.network.OnlinePlayers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Session;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.net.Proxy;
import java.util.concurrent.CompletableFuture;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin implements IMinecraftClient {
    @Shadow public ClientWorld world;

    @Shadow private int itemUseCooldown;

    @Shadow protected abstract void doItemUse();

    @Shadow protected abstract void doAttack();

    @Shadow @Final public Mouse mouse;

    @Shadow @Final private Window window;

    @Shadow @Final private Proxy netProxy;

    @Shadow @Final @Mutable private Session session;

    @Shadow private static int currentFps;

    @Shadow @Nullable public Screen currentScreen;

    @Shadow public abstract Profiler getProfiler();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        MeteorClient.INSTANCE.onInitializeClient();
    }

    @Inject(at = @At("HEAD"), method = "tick")
    private void onPreTick(CallbackInfo info) {
        OnlinePlayers.update();

        getProfiler().push("meteor-client_pre_update");
        MeteorClient.EVENT_BUS.post(TickEvent.Pre.get());
        getProfiler().pop();
    }

    @Inject(at = @At("TAIL"), method = "tick")
    private void onTick(CallbackInfo info) {
        getProfiler().push("meteor-client_post_update");
        MeteorClient.EVENT_BUS.post(TickEvent.Post.get());
        getProfiler().pop();
    }
    
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
    private void onDisconnect(Screen screen, CallbackInfo info) {
        if (world != null) {
            MeteorClient.EVENT_BUS.post(GameLeftEvent.get());
        }
    }

    @Inject(method = "openScreen", at = @At("HEAD"), cancellable = true)
    private void onOpenScreen(Screen screen, CallbackInfo info) {
        if (screen instanceof WidgetScreen) screen.mouseMoved(mouse.getX() * window.getScaleFactor(), mouse.getY() * window.getScaleFactor());

        OpenScreenEvent event = OpenScreenEvent.get(screen);
        MeteorClient.EVENT_BUS.post(event);

        if (event.isCancelled()) {
            info.cancel();
            return;
        }

        GuiKeyEvents.resetPostKeyEvents();
    }

    @Redirect(method = "doItemUse", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;crosshairTarget:Lnet/minecraft/util/hit/HitResult;", ordinal = 1))
    private HitResult doItemUseMinecraftClientCrosshairTargetProxy(MinecraftClient client) {
        if (Modules.get().get(AutoEat.class).rightClickThings() && Modules.get().get(AutoGap.class).rightClickThings()) return client.crosshairTarget;
        return null;
    }

    @ModifyVariable(method = "reloadResources", at = @At("STORE"), ordinal = 0)
    private CompletableFuture<Void> onReloadResourcesNewCompletableFuture(CompletableFuture<Void> completableFuture) {
        completableFuture.thenRun(() -> MeteorClient.EVENT_BUS.post(ResourcePacksReloadedEvent.get()));
        return completableFuture;
    }

    @Override
    public void leftClick() {
        doAttack();
    }

    @Override
    public void rightClick() {
        doItemUse();
    }

    @Override
    public void setItemUseCooldown(int cooldown) {
        itemUseCooldown = cooldown;
    }

    @Override
    public Proxy getProxy() {
        return netProxy;
    }

    @Override
    public void setSession(Session session) {
        this.session = session;
    }

    @Override
    public int getFps() {
        return currentFps;
    }
}
