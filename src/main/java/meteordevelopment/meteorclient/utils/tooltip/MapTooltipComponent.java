/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.tooltip;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.resources.Identifier;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MapTooltipComponent implements ClientTooltipComponent, MeteorTooltipData {
    private static final Identifier TEXTURE_MAP_BACKGROUND = Identifier.of("textures/map/map_background.png");
    private final int mapId;
    private final MapRenderState mapRenderState = new MapRenderState();

    public MapTooltipComponent(int mapId) {
        this.mapId = mapId;
    }

    @Override
    public int getHeight(Font textRenderer) {
        double scale = Modules.get().get(BetterTooltips.class).mapsScale.get();
        return (int) ((128 + 16) * scale) + 2;
    }

    @Override
    public int getWidth(Font textRenderer) {
        double scale = Modules.get().get(BetterTooltips.class).mapsScale.get();
        return (int) ((128 + 16) * scale);
    }

    @Override
    public ClientTooltipComponent getComponent() {
        return this;
    }

    @Override
    public void drawItems(Font textRenderer, int x, int y, int width, int height, GuiGraphics context) {
        var scale = Modules.get().get(BetterTooltips.class).mapsScale.get().floatValue();

        // Background
        int size = (int) ((128 + 16) * scale);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE_MAP_BACKGROUND, x, y, 0, 0, size, size, size, size);

        // Contents
        MapItemSavedData mapState = MapItem.getMapState(new MapIdComponent(mapId), mc.world);
        if (mapState == null) return;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(scale, scale);
        context.getMatrices().translate(8, 8);

        mc.getMapRenderer().update(new MapIdComponent(mapId), mapState, mapRenderState);
        context.drawMap(mapRenderState);

        context.getMatrices().popMatrix();
    }
}
