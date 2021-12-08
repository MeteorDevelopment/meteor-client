/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.tooltip;

import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BannerItem;
import net.minecraft.item.ItemStack;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BannerTooltipComponent implements MeteorTooltipData, TooltipComponent {
    private final ItemStack banner;
    private final ModelPart bannerField;

    public BannerTooltipComponent(ItemStack banner) {
        this.banner = banner;
        this.bannerField = mc.getEntityModelLoader().getModelPart(EntityModelLayers.BANNER).getChild("flag");
    }

    @Override
    public TooltipComponent getComponent() {
        return this;
    }

    @Override
    public int getHeight() {
        return 32 * 5 - 2;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return 16 * 5;
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, MatrixStack matrices, ItemRenderer itemRenderer, int z) {
        DiffuseLighting.disableGuiDepthLighting();
        matrices.push();
        matrices.translate(x + 8, y + 8, z);

        matrices.push();
        matrices.translate(0.5, 16, 0);
        matrices.scale(6, -6, 1);
        matrices.scale(2, -2, -2);
        matrices.push();
        matrices.translate(2.5, 8.5, 0);
        matrices.scale(5, 5, 5);
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        this.bannerField.pitch = 0f;
        this.bannerField.pivotY = -32f;
        BannerBlockEntityRenderer.renderCanvas(
            matrices,
            immediate,
            0xF000F0,
            OverlayTexture.DEFAULT_UV,
            this.bannerField,
            ModelLoader.BANNER_BASE,
            true,
            BannerBlockEntity.getPatternsFromNbt(
                ((BannerItem) this.banner.getItem()).getColor(),
                BannerBlockEntity.getPatternListTag(this.banner)
            )
        );
        matrices.pop();
        matrices.pop();
        immediate.draw();
        matrices.pop();
        DiffuseLighting.enableGuiDepthLighting();
    }
}
