/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.modules.render.hud.HudEditorScreen;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.TickRate;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;

public class LagNotifierHud extends DoubleTextHudModule {
    private static final Color RED = new Color(225, 45, 45);
    private static final Color AMBER = new Color(235, 158, 52);
    private static final Color YELLOW = new Color(255, 255, 5);

    public LagNotifierHud(HUD hud) {
        super(hud, "lag-notifier", "Displays if the server is lagging.", "Time since last tick ");
    }

    @Override
    protected String getRight() {
        if (!Utils.canUpdate()) {
            rightColor = RED;
            visible = true;
            return "4,3";
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        float timeSinceLastTick = TickRate.INSTANCE.getTimeSinceLastTick();

        if (timeSinceLastTick > 10) rightColor = RED;
        else if (timeSinceLastTick > 3) rightColor = AMBER;
        else rightColor = YELLOW;

        visible = timeSinceLastTick >= 1f || mc.currentScreen instanceof HudEditorScreen;
        return String.format("%.1f", timeSinceLastTick);
    }
}
