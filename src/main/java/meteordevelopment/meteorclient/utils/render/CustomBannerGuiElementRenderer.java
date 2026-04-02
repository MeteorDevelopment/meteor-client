/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.client.resources.model.ModelBakery;

public class CustomBannerGuiElementRenderer extends PictureInPictureRenderer<CustomBannerGuiElementRenderState> {
    private final MaterialSet sprite;

    public CustomBannerGuiElementRenderer(MultiBufferSource.BufferSource immediate, MaterialSet sprite) {
        super(immediate);
        this.sprite = sprite;
    }

    @Override
    public Class<CustomBannerGuiElementRenderState> getRenderStateClass() {
        return CustomBannerGuiElementRenderState.class;
    }

    protected void renderToTexture(CustomBannerGuiElementRenderState state, PoseStack matrixStack) {
        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_FLAT);
        matrixStack.translate(0.0F, 0.25F, 0.0F);
        FeatureRenderDispatcher renderDispatcher = Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher();
        SubmitNodeStorage orderedRenderCommandQueueImpl = renderDispatcher.getSubmitNodeStorage();
        BannerRenderer.submitPatterns(
            this.sprite,
            matrixStack,
            orderedRenderCommandQueueImpl,
            15728880,
            OverlayTexture.NO_OVERLAY,
            state.flag(),
            0.0F,
            ModelBakery.BANNER_BASE,
            true,
            state.baseColor(),
            state.resultBannerPatterns(),
            false,
            null,
            0
        );
        renderDispatcher.renderAllFeatures();
    }

    @Override
    protected String getTextureLabel() {
        return "custom banner";
    }
}
