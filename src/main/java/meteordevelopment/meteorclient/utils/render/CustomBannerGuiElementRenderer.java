/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import org.jspecify.annotations.NonNull;

public class CustomBannerGuiElementRenderer extends PictureInPictureRenderer<CustomBannerGuiElementRenderState> {
    private final SpriteGetter sprites;

    public CustomBannerGuiElementRenderer(SpriteGetter sprites) {
        this.sprites = sprites;
    }

    @Override
    public Class<CustomBannerGuiElementRenderState> getRenderStateClass() {
        return CustomBannerGuiElementRenderState.class;
    }

    protected void renderToTexture(CustomBannerGuiElementRenderState state, PoseStack matrixStack, SubmitNodeCollector submitNodeCollector) {
        Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_FLAT);
        matrixStack.translate(0.0F, 0.25F, 0.0F);
        BannerRenderer.submitPatterns(
            this.sprites,
            matrixStack,
            submitNodeCollector,
            15728880,
            OverlayTexture.NO_OVERLAY,
            state.flag(),
            0.0F,
            true,
            state.baseColor(),
            state.resultBannerPatterns(),
            null
        );
    }

    @Override
    protected @NonNull String getTextureLabel() {
        return "custom banner";
    }
}
