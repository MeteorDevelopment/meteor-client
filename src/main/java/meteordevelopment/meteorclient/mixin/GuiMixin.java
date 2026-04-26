/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void onExtractStatusEffectOverlay(CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noPotionIcons()) ci.cancel();
    }

    @Inject(method = "extractPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void onExtractPortalOverlay(GuiGraphicsExtractor graphics, float alpha, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noPortalOverlay()) ci.cancel();
    }

    @ModifyArgs(method = "extractCameraOverlays", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;extractTextureOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/resources/Identifier;F)V", ordinal = 0))
    private void onExtractPumpkinOverlay(Args args) {
        if (Modules.get().get(NoRender.class).noPumpkinOverlay()) args.set(2, 0f);
    }

    @ModifyArgs(method = "extractCameraOverlays", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;extractTextureOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/resources/Identifier;F)V", ordinal = 1))
    private void onExtractPowderedSnowOverlay(Args args) {
        if (Modules.get().get(NoRender.class).noPowderedSnowOverlay()) args.set(2, 0f);
    }

    @Inject(method = "extractVignette", at = @At("HEAD"), cancellable = true)
    private void onExtractVignetteOverlay(GuiGraphicsExtractor graphics, Entity camera, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noVignette()) ci.cancel();
    }

    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void onExtractScoreboardSidebar(GuiGraphicsExtractor graphics, Objective objective, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noScoreboard()) ci.cancel();
    }

    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void onExtractScoreboardSidebar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noScoreboard()) ci.cancel();
    }

    @Inject(method = "extractSpyglassOverlay", at = @At("HEAD"), cancellable = true)
    private void onExtractSpyglassOverlay(GuiGraphicsExtractor graphics, float scale, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noSpyglassOverlay()) ci.cancel();
    }

    @ModifyExpressionValue(method = "extractCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z"))
    private boolean alwaysRenderCrosshairInFreecam(boolean firstPerson) {
        return Modules.get().isActive(Freecam.class) || firstPerson;
    }

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void onExtractCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noCrosshair()) ci.cancel();
    }

    @Inject(method = "extractTitle", at = @At("HEAD"), cancellable = true)
    private void onExtractTitle(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noTitle()) ci.cancel();
    }

    @Inject(method = "extractSelectedItemName", at = @At("HEAD"), cancellable = true)
    private void onExtractHeldItemTooltip(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noHeldItemName()) ci.cancel();
    }

    @Inject(method = "onDisconnected", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;clearMessages(Z)V"), cancellable = true)
    private void onClear(CallbackInfo ci) {
        if (Modules.get().get(BetterChat.class).keepHistory()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractConfusionOverlay", at = @At("HEAD"), cancellable = true)
    private void onExtractNausea(GuiGraphicsExtractor graphics, float strength, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noNausea()) ci.cancel();
    }
}
