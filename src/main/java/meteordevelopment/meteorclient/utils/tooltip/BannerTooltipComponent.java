/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.tooltip;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.item.BannerItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DyeColor;
import org.joml.Matrix3x2fStack;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BannerTooltipComponent implements MeteorTooltipData, TooltipComponent {
    private final DyeColor color;
    private final BannerPatternsComponent patterns;
    private final ModelPart bannerField;

    // should only be used when the ItemStack is a banner
    public BannerTooltipComponent(ItemStack banner) {
        this.color = ((BannerItem) banner.getItem()).getColor();
        this.patterns = banner.getOrDefault(DataComponentTypes.BANNER_PATTERNS, BannerPatternsComponent.DEFAULT);
        this.bannerField = mc.getLoadedEntityModels().getModelPart(EntityModelLayers.STANDING_BANNER_FLAG).getChild("flag");
    }

    public BannerTooltipComponent(DyeColor color, BannerPatternsComponent patterns) {
        this.color = color;
        this.patterns = patterns;
        this.bannerField = mc.getLoadedEntityModels().getModelPart(EntityModelLayers.STANDING_BANNER_FLAG).getChild("flag");
    }

    @Override
    public TooltipComponent getComponent() {
        return this;
    }

    @Override
    public int getHeight(TextRenderer textRenderer) {
        return 32 * 5 -2;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return 16 * 5;
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
        // todo ????
        mc.gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.ITEMS_FLAT);

        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x + 8, y + 8);

        matrices.pushMatrix();
        matrices.translate(0.5f, 16f);
        matrices.scale(6, -6);
        matrices.scale(2, -2);
        matrices.pushMatrix();
        matrices.translate(2.5f, 8.5f);
        matrices.scale(5, 5);
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        bannerField.pitch = 0f;
        bannerField.originY = -32f;
        BannerBlockEntityRenderer.renderCanvas(
            new MatrixStack(), // todo fix differing matrixstacks
            immediate,
            0xF000F0,
            OverlayTexture.DEFAULT_UV,
            bannerField,
            ModelBaker.BANNER_BASE,
            true,
            color,
            patterns
        );
        matrices.popMatrix();
        matrices.popMatrix();
        immediate.draw();
        matrices.popMatrix();
    }
}
