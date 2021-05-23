/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.hud.elements;

import minegame159.meteorclient.systems.hud.DoubleTextHudElement;
import minegame159.meteorclient.systems.hud.ElementRegister;
import minegame159.meteorclient.utils.world.TickRate;

@ElementRegister(name = "tps")
public class TpsHud extends DoubleTextHudElement {
    public TpsHud() {
        super("tps", "Displays the server's TPS.", "TPS: ");
    }

    @Override
    protected String getRight() {
        return String.format("%.1f", TickRate.INSTANCE.getTickRate());
    }
}
