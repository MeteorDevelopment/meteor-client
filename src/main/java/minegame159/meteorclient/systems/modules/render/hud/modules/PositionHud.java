/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render.hud.modules;

import minegame159.meteorclient.systems.modules.render.hud.HUD;
import minegame159.meteorclient.systems.modules.render.hud.HudRenderer;
import minegame159.meteorclient.utils.Utils;

public class PositionHud extends HudElement {
    private final String left1 = "Pos: ";
    private double left1Width;
    private String right1;

    private String left2;
    private double left2Width;
    private String right2;

    public PositionHud(HUD hud) {
        super(hud, "coords", "Displays your coordinates in the world.");
    }

    @Override
    public void update(HudRenderer renderer) {
        left1Width = renderer.textWidth(left1);
        left2 = null;

        if (isInEditor()) {
            right1 = "0,0 0,0 0,0";
            box.setSize(left1Width + renderer.textWidth(right1), renderer.textHeight() * 2 + 2);
            return;
        }

        double x1 = mc.gameRenderer.getCamera().getPos().x;
        double y1 = mc.gameRenderer.getCamera().getPos().y - mc.player.getEyeHeight(mc.player.getPose());
        double z1 = mc.gameRenderer.getCamera().getPos().z;

        right1 = String.format("%.1f %.1f %.1f", x1, y1, z1);

        switch (Utils.getDimension()) {
            case Overworld:
                left2 = "Nether Pos: ";
                right2 = String.format("%.1f %.1f %.1f", x1 / 8.0, y1, z1 / 8.0);
                break;

            case Nether:
                left2 = "Overworld Pos: ";
                right2 = String.format("%.1f %.1f %.1f", x1 * 8.0, y1, z1 * 8.0);
                break;
        }

        double width = left1Width + renderer.textWidth(right1);

        if (left2 != null) {
            left2Width = renderer.textWidth(left2);
            width = Math.max(width, left2Width + renderer.textWidth(right2));
        }

        box.setSize(width, renderer.textHeight() * 2 + 2);
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();

        if (left2 != null) {
            renderer.text(left2, x, y, hud.primaryColor.get());
            renderer.text(right2, x + left2Width, y, hud.secondaryColor.get());
        }

        double xOffset = box.alignX(left1Width + renderer.textWidth(right1));
        double yOffset = renderer.textHeight() + 2;

        renderer.text(left1, x + xOffset, y + yOffset, hud.primaryColor.get());
        renderer.text(right1, x + xOffset + left1Width, y + yOffset, hud.secondaryColor.get());
    }
}
