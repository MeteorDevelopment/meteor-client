/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.mixininterface.IGameRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(Gui.class)
public abstract class InGameHudMixin {
    @Shadow public abstract void onDisconnected();

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onRender(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        // The old immediate 2D pass interferes with 26.1's GUI extraction/render state flow.
        // Keep vanilla HUD extraction intact until the full 2D overlay path is ported properly.
    }

    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void onRenderStatusEffectOverlay(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo info) {
        if (Modules.get().get(NoRender.class).noPotionIcons()) info.cancel();
    }

    @Inject(method = "extractPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderPortalOverlay(GuiGraphicsExtractor context, float nauseaStrength, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noPortalOverlay()) ci.cancel();
    }

    @ModifyArgs(method = "extractCameraOverlays", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;extractTextureOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/resources/Identifier;F)V", ordinal = 0))
    private void onRenderPumpkinOverlay(Args args) {
        if (Modules.get().get(NoRender.class).noPumpkinOverlay()) args.set(2, 0f);
    }

    @ModifyArgs(method = "extractCameraOverlays", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;extractTextureOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/resources/Identifier;F)V", ordinal = 1))
    private void onRenderPowderedSnowOverlay(Args args) {
        if (Modules.get().get(NoRender.class).noPowderedSnowOverlay()) args.set(2, 0f);
    }

    @Inject(method = "extractVignette", at = @At("HEAD"), cancellable = true)
    private void onRenderVignetteOverlay(GuiGraphicsExtractor context, Entity entity, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noVignette()) ci.cancel();
    }

    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void onRenderScoreboardSidebar(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noScoreboard()) ci.cancel();
    }

    @Inject(method = "extractSpyglassOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderSpyglassOverlay(GuiGraphicsExtractor context, float scale, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noSpyglassOverlay()) ci.cancel();
    }

    @ModifyExpressionValue(method = "extractCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z"))
    private boolean alwaysRenderCrosshairInFreecam(boolean firstPerson) {
        return Modules.get().isActive(Freecam.class) || firstPerson;
    }

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void onRenderCrosshair(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noCrosshair()) ci.cancel();
    }

    @Inject(method = "extractTitle", at = @At("HEAD"), cancellable = true)
    private void onRenderTitle(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noTitle()) ci.cancel();
    }

    @Inject(method = "extractSelectedItemName", at = @At("HEAD"), cancellable = true)
    private void onRenderHeldItemTooltip(GuiGraphicsExtractor context, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noHeldItemName()) ci.cancel();
    }

    @Inject(method = "onDisconnected", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;clearMessages(Z)V"), cancellable = true)
    private void onClear(CallbackInfo info) {
        if (Modules.get().get(BetterChat.class).keepHistory()) {
            info.cancel();
        }
    }

    @Inject(method = "extractConfusionOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderNausea(GuiGraphicsExtractor context, float distortionStrength, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noNausea()) ci.cancel();
    }
}
