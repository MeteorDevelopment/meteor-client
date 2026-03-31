/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

public record CustomBannerGuiElementRenderState(
    BannerFlagModel flag,
    DyeColor baseColor,
    BannerPatternLayers resultBannerPatterns,
    int x1,
    int y1,
    int x2,
    int y2,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds,
    float scale
) implements PictureInPictureRenderState {
    public CustomBannerGuiElementRenderState(
        BannerFlagModel bannerFlagBlockModel,
        DyeColor color,
        BannerPatternLayers bannerPatterns,
        int x1,
        int y1,
        int x2,
        int y2,
        @Nullable ScreenRectangle scissorArea,
        float scale
    ) {
        this(bannerFlagBlockModel, color, bannerPatterns, x1, y1, x2, y2, scissorArea, PictureInPictureRenderState.createBounds(x1, y1, x2, y2, scissorArea), scale);
    }
}
