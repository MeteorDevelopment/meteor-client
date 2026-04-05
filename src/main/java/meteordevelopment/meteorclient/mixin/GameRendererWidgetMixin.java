/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererWidgetMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private GuiRenderer guiRenderer;

    @Shadow
    @Final
    private FogRenderer fogRenderer;

    @Shadow
    @Final
    private GameRenderState gameRenderState;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = At.Shift.AFTER))
    private void meteor$renderWidgetScreens(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        if (!(minecraft.screen instanceof WidgetScreen widgetScreen)) return;

        GuiRenderState guiRenderState = gameRenderState.guiRenderState;
        guiRenderState.reset();

        int mouseX = (int) minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
        int mouseY = (int) minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());

        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(minecraft, guiRenderState, mouseX, mouseY);
        widgetScreen.renderCustom(graphics, mouseX, mouseY, deltaTracker.getGameTimeDeltaTicks());

        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(minecraft.getMainRenderTarget().getDepthTexture(), 1.0);
        guiRenderer.render(fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
        guiRenderer.endFrame();
    }
}
