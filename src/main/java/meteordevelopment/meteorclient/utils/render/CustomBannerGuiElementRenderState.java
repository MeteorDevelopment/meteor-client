/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;
import net.minecraft.client.render.block.entity.model.BannerFlagBlockModel;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.util.DyeColor;
import org.jetbrains.annotations.Nullable;

public record CustomBannerGuiElementRenderState(
    BannerFlagBlockModel flag,
    DyeColor baseColor,
    BannerPatternsComponent resultBannerPatterns,
    int x1,
    int y1,
    int x2,
    int y2,
    @Nullable ScreenRect scissorArea,
    @Nullable ScreenRect bounds,
    float scale
) implements SpecialGuiElementRenderState {
    public CustomBannerGuiElementRenderState(
        BannerFlagBlockModel bannerFlagBlockModel,
        DyeColor color,
        BannerPatternsComponent bannerPatterns,
        int x1,
        int y1,
        int x2,
        int y2,
        @Nullable ScreenRect scissorArea,
        float scale
    ) {
        this(bannerFlagBlockModel, color, bannerPatterns, x1, y1, x2, y2, scissorArea, SpecialGuiElementRenderState.createBounds(x1, y1, x2, y2, scissorArea), scale);
    }
}
