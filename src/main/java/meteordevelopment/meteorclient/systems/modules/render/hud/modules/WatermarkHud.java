/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.hud.modules;

import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUD;

public class WatermarkHud extends DoubleTextHudElement {
    public WatermarkHud(HUD hud) {
        super(hud, "watermark", "Displays a Meteor Client watermark.", "Meteor Client ");
    }

    @Override
    protected String getRight() {
        if (Config.get().devBuild.isEmpty()) {
            return Config.get().version.getOriginalString();
        }

        return Config.get().version.getOriginalString() + " " + Config.get().devBuild;
    }
}
