/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.modules;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;

public class PositionHud extends HudElement {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> accurate = sgGeneral.add(new BoolSetting.Builder()
        .name("accurate")
        .description("Shows position with decimal points.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> oppositeDim = sgGeneral.add(new BoolSetting.Builder()
        .name("opposite-dimension")
        .description("Displays the coords of the opposite dimension (Nether or Overworld).")
        .defaultValue(false)
        .build()
    );

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
        right2 = null;

        double height = renderer.textHeight();
        if (oppositeDim.get()) height = height * 2 + 2;

        if (isInEditor()) {
            right1 = "0, 0, 0";
            box.setSize(left1Width + renderer.textWidth(right1), height);
            return;
        }

        Freecam freecam = Modules.get().get(Freecam.class);

        double x, y, z;

        if (accurate.get()) {
            x = freecam.isActive() ? mc.gameRenderer.getCamera().getPos().x : mc.player.getX();
            y = freecam.isActive() ?
                mc.gameRenderer.getCamera().getPos().y - mc.player.getEyeHeight(mc.player.getPose()) :
                mc.player.getY();
            z = freecam.isActive() ? mc.gameRenderer.getCamera().getPos().z : mc.player.getZ();

            right1 = String.format("%.1f %.1f %.1f", x, y, z);
        }
        else {
            x = freecam.isActive() ? mc.gameRenderer.getCamera().getBlockPos().getX() : mc.player.getBlockX();
            y = freecam.isActive() ? mc.gameRenderer.getCamera().getBlockPos().getY() : mc.player.getBlockY();
            z = freecam.isActive() ? mc.gameRenderer.getCamera().getBlockPos().getZ() : mc.player.getBlockZ();

            right1 = String.format("%d %d %d", (int) x, (int) y, (int) z);
        }

        if (oppositeDim.get()) {
            switch (PlayerUtils.getDimension()) {
                case Overworld -> {
                    left2 = "Nether Pos: ";
                    right2 = accurate.get() ?
                        String.format("%.1f %.1f %.1f", x / 8.0, y, z / 8.0) :
                        String.format("%d %d %d", (int) x / 8, (int) y, (int) z / 8);
                }
                case Nether -> {
                    left2 = "Overworld Pos: ";
                    right2 = accurate.get() ?
                        String.format("%.1f %.1f %.1f", x * 8.0, y, z * 8.0) :
                        String.format("%d %d %d", (int) x * 8, (int) y, (int) z * 8);
                }
            }
        }

        double width = left1Width + renderer.textWidth(right1);

        if (left2 != null) {
            left2Width = renderer.textWidth(left2);
            width = Math.max(width, left2Width + renderer.textWidth(right2));
        }

        box.setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();

        double xOffset = box.alignX(left1Width + renderer.textWidth(right1));
        double yOffset = 0;

        if (left2 != null) {
            renderer.text(left2, x, y, hud.primaryColor.get());
            renderer.text(right2, x + left2Width, y, hud.secondaryColor.get());
            yOffset = renderer.textHeight() + 2;
        }

        renderer.text(left1, x + xOffset, y + yOffset, hud.primaryColor.get());
        renderer.text(right1, x + xOffset + left1Width, y + yOffset, hud.secondaryColor.get());
    }
}
