/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.modules;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.hud.HUD;

public class WatermarkHud extends DoubleTextHudElement {
    public WatermarkHud(HUD hud) {
        super(hud, "watermark", "Displays a Meteor Client watermark.", "Meteor Client ");
    }

    @Override
    protected String getRight() {
        if (MeteorClient.DEV_BUILD.isEmpty()) {
            return MeteorClient.VERSION.toString();
        }

        return MeteorClient.VERSION + " " + MeteorClient.DEV_BUILD;
    }
}
