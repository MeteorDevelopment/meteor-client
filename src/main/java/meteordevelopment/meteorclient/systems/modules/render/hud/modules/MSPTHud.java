/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2022 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.hud.modules;

import meteordevelopment.meteorclient.systems.modules.render.hud.HUD;
import meteordevelopment.meteorclient.utils.world.TickRate;

public class MSPTHud extends DoubleTextHudElement {
    public MSPTHud(HUD hud) {
        super(hud, "MSTP", "Displays the time in between ticks in milliseconds.", "MSPT: ");
    }
    @Override
    protected String getRight() {
        return String.format("%.1f", TickRate.INSTANCE.getMSPT());
    }
}
