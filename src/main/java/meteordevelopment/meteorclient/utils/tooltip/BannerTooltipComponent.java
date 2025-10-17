/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.tooltip;

import meteordevelopment.meteorclient.utils.render.CustomBannerGuiElementRenderState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.block.entity.model.BannerFlagBlockModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.item.BannerItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DyeColor;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BannerTooltipComponent implements MeteorTooltipData, TooltipComponent {
    private final DyeColor color;
    private final BannerPatternsComponent patterns;
    private final BannerFlagBlockModel bannerFlag;

    /** Should only be used when the ItemStack is a banner */
    public BannerTooltipComponent(ItemStack banner) {
        this.color = ((BannerItem) banner.getItem()).getColor();
        this.patterns = banner.getOrDefault(DataComponentTypes.BANNER_PATTERNS, BannerPatternsComponent.DEFAULT);
        ModelPart modelPart = mc.getLoadedEntityModels().getModelPart(EntityModelLayers.STANDING_BANNER_FLAG);
        this.bannerFlag = new BannerFlagBlockModel(modelPart);
    }

    public BannerTooltipComponent(DyeColor color, BannerPatternsComponent patterns) {
        this.color = color;
        this.patterns = patterns;
        ModelPart modelPart = mc.getLoadedEntityModels().getModelPart(EntityModelLayers.STANDING_BANNER_FLAG);
        this.bannerFlag = new BannerFlagBlockModel(modelPart);
    }

    @Override
    public TooltipComponent getComponent() {
        return this;
    }

    @Override
    public int getHeight(TextRenderer textRenderer) {
        return 40 * 2;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return 20 * 2;
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
        var centerX = width / 2 - getWidth(null) / 2;

        context.state.addSpecialElement(new CustomBannerGuiElementRenderState(
            bannerFlag, color, patterns,
            centerX + x, y,
            centerX + x + getWidth(null), y + getHeight(null),
            context.scissorStack.peekLast(),
            16 * 2
        ));
    }
}
