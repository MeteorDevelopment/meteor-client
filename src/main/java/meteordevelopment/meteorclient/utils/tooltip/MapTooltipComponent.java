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
import org.joml.Matrix3x2fStack;

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
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale((float) (scale) * 2, (float) (scale) * 2);
        matrices.scale((64 + 8) / 64f, (64 + 8) / 64f);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE_MAP_BACKGROUND, 0, 0, 0, 0, 0, 64, 64, 64, 64);
        matrices.popMatrix();

        // Contents
        VertexConsumerProvider.Immediate consumer = mc.getBufferBuilders().getEntityVertexConsumers();
        MapState mapState = FilledMapItem.getMapState(new MapIdComponent(mapId), mc.world);
        if (mapState == null) return;
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale((float) scale, (float) scale);
        matrices.translate(8, 8);
        mc.getMapRenderer().update(new MapIdComponent(mapId), mapState, mapRenderState);
        // todo fix differing matrixstacks
        mc.getMapRenderer().draw(mapRenderState, new MatrixStack(), consumer, false, 0xF000F0);
        consumer.draw();
        matrices.popMatrix();
    }
}
