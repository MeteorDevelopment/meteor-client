/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.tooltip;

import meteordevelopment.meteorclient.mixin.GuiGraphicsAccessor;
import meteordevelopment.meteorclient.utils.render.CustomBannerGuiElementRenderState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BannerTooltipComponent implements MeteorTooltipData, ClientTooltipComponent {
    private final DyeColor color;
    private final BannerPatternLayers patterns;
    private final BannerFlagModel bannerFlag;

    /**
     * Should only be used when the ItemStack is a banner
     */
    public BannerTooltipComponent(ItemStack banner) {
        this.color = ((BannerItem) banner.getItem()).getColor();
        this.patterns = banner.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
        ModelPart modelPart = mc.getEntityModels().bakeLayer(ModelLayers.STANDING_BANNER_FLAG);
        this.bannerFlag = new BannerFlagModel(modelPart);
    }

    public BannerTooltipComponent(DyeColor color, BannerPatternLayers patterns) {
        this.color = color;
        this.patterns = patterns;
        ModelPart modelPart = mc.getEntityModels().bakeLayer(ModelLayers.STANDING_BANNER_FLAG);
        this.bannerFlag = new BannerFlagModel(modelPart);
    }

    @Override
    public ClientTooltipComponent getComponent() {
        return this;
    }

    @Override
    public int getHeight(Font textRenderer) {
        return 40 * 2;
    }

    @Override
    public int getWidth(Font textRenderer) {
        return 20 * 2;
    }

    @Override
    public void renderImage(Font textRenderer, int x, int y, int width, int height, GuiGraphics context) {
        var centerX = width / 2 - getWidth(null) / 2;

        GuiGraphicsAccessor contextAccessor = (GuiGraphicsAccessor) context;

        contextAccessor.getGuiRenderState().submitPicturesInPictureState(new CustomBannerGuiElementRenderState(
            bannerFlag, color, patterns,
            centerX + x, y,
            centerX + x + getWidth(null), y + getHeight(null),
            contextAccessor.getScissorStack().peek(),
            16 * 2
        ));
    }
}
