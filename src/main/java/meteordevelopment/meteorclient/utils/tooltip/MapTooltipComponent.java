/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Identifier;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MapTooltipComponent implements TooltipComponent, MeteorTooltipData {
    private static final Identifier TEXTURE_MAP_BACKGROUND = new Identifier("textures/map/map_background.png");
    private final int mapId;

    public MapTooltipComponent(int mapId) {
        this.mapId = mapId;
    }

    @Override
    public int getHeight() {
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
    public void drawItems(TextRenderer textRenderer, int x, int y, MatrixStack matrices, ItemRenderer itemRenderer, int z) {
        double scale = Modules.get().get(BetterTooltips.class).mapsScale.get();

        // Background
        matrices.push();
        matrices.translate(x, y, z);
        matrices.scale((float) (scale) * 2, (float) (scale) * 2, 0);
        matrices.scale((64 + 8) / 64f, (64 + 8) / 64f, 0);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, TEXTURE_MAP_BACKGROUND);
        DrawableHelper.drawTexture(matrices, 0, 0, 0, 0, 0, 64, 64, 64, 64);
        matrices.pop();

        // Contents
        VertexConsumerProvider.Immediate consumer = mc.getBufferBuilders().getEntityVertexConsumers();
        MapState mapState = FilledMapItem.getMapState(this.mapId, mc.world);
        if (mapState == null) return;
        matrices.push();
        matrices.translate(x, y, z);
        matrices.scale((float) scale, (float) scale, 0);
        matrices.translate(8, 8, 0);
        mc.gameRenderer.getMapRenderer().draw(matrices, consumer, this.mapId, mapState, false, 0xF000F0);
        consumer.draw();
        matrices.pop();
    }
}
