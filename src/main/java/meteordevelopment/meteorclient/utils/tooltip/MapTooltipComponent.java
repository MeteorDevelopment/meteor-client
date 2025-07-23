/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.tooltip;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.MapRenderState;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Identifier;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MapTooltipComponent implements TooltipComponent, MeteorTooltipData {
    private static final Identifier TEXTURE_MAP_BACKGROUND = Identifier.of("textures/map/map_background.png");
    private final int mapId;
    private final MapRenderState mapRenderState = new MapRenderState();

    public MapTooltipComponent(int mapId) {
        this.mapId = mapId;
    }

    @Override
    public int getHeight(TextRenderer textRenderer) {
        double scale = Modules.get().get(BetterTooltips.class).mapsScale.get();
        return (int) ((128 + 16) * scale) + 2;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        double scale = Modules.get().get(BetterTooltips.class).mapsScale.get();
        return (int) ((128 + 16) * scale);
    }

    @Override
    public TooltipComponent getComponent() {
        return this;
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
        double scale = Modules.get().get(BetterTooltips.class).mapsScale.get();

        // Background
        int size = (int) ((128 + 16) * scale);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE_MAP_BACKGROUND, x, y, 0,0, size, size, size, size);

        // Contents
        MapState mapState = FilledMapItem.getMapState(new MapIdComponent(mapId), mc.world);
        if (mapState == null) return;

        MatrixStack matrices2 = new MatrixStack();
        VertexConsumerProvider.Immediate consumer = mc.getBufferBuilders().getEntityVertexConsumers();

        matrices2.push();
        matrices2.translate(x, y, 0);
        matrices2.scale((float) scale, (float) scale, 0);
        matrices2.translate(8, 8, 0);
        mc.getMapRenderer().update(new MapIdComponent(mapId), mapState, mapRenderState);
        mc.getMapRenderer().draw(mapRenderState, matrices2, consumer, false, 0xF000F0);
        consumer.draw();
        matrices2.pop();
    }
}
