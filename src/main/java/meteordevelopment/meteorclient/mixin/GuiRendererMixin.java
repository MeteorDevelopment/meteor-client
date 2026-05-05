/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.MeteorMcGuiRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.util.profiling.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    @Unique
    private GuiRenderState renderState;

    @Unique
    private MeteorMcGuiRenderer guiRenderer;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init$meteor(GuiRenderState renderState, MultiBufferSource.BufferSource bufferSource, SubmitNodeCollector submitNodeCollector, FeatureRenderDispatcher featureRenderDispatcher, List<PictureInPictureRenderer<?>> pictureInPictureRenderers, CallbackInfo ci) {
        if ((GuiRenderer) (Object) this instanceof MeteorMcGuiRenderer) return;

        this.renderState = new GuiRenderState();

        guiRenderer = new MeteorMcGuiRenderer(
            this.renderState,
            bufferSource,
            submitNodeCollector,
            featureRenderDispatcher,
            pictureInPictureRenderers
        );
    }

    @Inject(method = "draw", at = @At("HEAD"))
    private void draw$executeDrawRange(CallbackInfo ci) {
        if ((GuiRenderer) (Object) this instanceof MeteorMcGuiRenderer) return;
        var mc = Minecraft.getInstance();

        var mouseX = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        var mouseY = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());

        var fogRenderer = ((GameRendererAccessor) mc.gameRenderer).meteor$fogRenderer();
        var delta = mc.getDeltaTracker().getGameTimeDeltaTicks();

        if (Utils.canUpdate()) {
            Profiler.get().push(MeteorClient.MOD_ID + "_render_2d");

            Utils.unscaledProjection();

            var graphics = new GuiGraphicsExtractor(mc, renderState, mouseX, mouseY);
            MeteorClient.EVENT_BUS.post(Render2DEvent.get(graphics, graphics.guiWidth(), graphics.guiWidth(), delta));
            guiRenderer.render(fogRenderer.getBuffer(FogRenderer.FogMode.NONE));

            Utils.scaledProjection();

            Profiler.get().pop();
        }

        if (mc.screen instanceof WidgetScreen widgetScreen) {
            var graphics = new GuiGraphicsExtractor(mc, renderState, mouseX, mouseY);
            widgetScreen.renderCustom(graphics, mouseX, mouseY, delta);
            guiRenderer.render(fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
        }

        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(mc.getMainRenderTarget().getDepthTexture(), 1.0);
        guiRenderer.endFrame();
    }
}
