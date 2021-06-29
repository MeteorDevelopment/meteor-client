/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.tooltip;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.map.MapState;

public class MapTooltipComponent implements TooltipComponent, MeteorTooltipData {
    private final int mapId;

    public MapTooltipComponent(int mapId) {
        this.mapId = mapId;
    }

    @Override
    public int getHeight() {
        double scale = Modules.get().get(BetterTooltips.class).mapsScale.get();
        return (int)(128*scale) + 2;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        double scale = Modules.get().get(BetterTooltips.class).mapsScale.get();
        return (int)(128*scale);
    }

    @Override
    public TooltipComponent getComponent() { return this; }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, MatrixStack matrices, ItemRenderer itemRenderer, int z, TextureManager textureManager) {
        double scale = Modules.get().get(BetterTooltips.class).mapsScale.get();

        // Contents
        VertexConsumerProvider.Immediate consumer = mc.getBufferBuilders().getEntityVertexConsumers();
        MapState mapState = FilledMapItem.getMapState(this.mapId, mc.world);
        if (mapState == null) return;
        matrices.push();
        matrices.translate(x, y, z);
        matrices.scale((float)scale, (float)scale, 0);
        mc.gameRenderer.getMapRenderer().draw(matrices, consumer, this.mapId, mapState, false, 0xF000F0);
        matrices.pop();
    }
}
