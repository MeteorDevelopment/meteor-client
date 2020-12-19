/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.modules.render.hud.HudRenderer;
import minegame159.meteorclient.rendering.DrawMode;
import minegame159.meteorclient.rendering.Renderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.VertexFormats;

public class PlayerModelHud extends HudModule {
    public PlayerModelHud(HUD hud) {
        super(hud, "player-model", "Displays a model of your player.");
    }

    @Override
    public void update(HudRenderer renderer) {
        box.setSize(51 * hud.playerModelScale(), 75 * hud.playerModelScale());
    }

    @Override
    public void render(HudRenderer renderer) {
        MinecraftClient mc = MinecraftClient.getInstance();

        int x = box.getX();
        int y = box.getY();

        if (hud.playerModelBackground()) {
            Renderer.NORMAL.begin(null, DrawMode.Triangles, VertexFormats.POSITION_COLOR);
            Renderer.NORMAL.quad(x, y, box.width, box.height, hud.playerModelColor());
            Renderer.NORMAL.end();
        }

        if (mc.player != null) {
            InventoryScreen.drawEntity(x + (int) (25 * hud.playerModelScale()), y + (int) (66 * hud.playerModelScale()), (int) (30 * hud.playerModelScale()), 0, 0, mc.player);
        }
    }
}
