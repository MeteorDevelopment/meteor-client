/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.hud.elements;

import minegame159.meteorclient.mixin.MinecraftClientAccessor;
import minegame159.meteorclient.systems.hud.DoubleTextHudElement;
import minegame159.meteorclient.systems.hud.ElementRegister;

@ElementRegister(name = "fps")
public class FpsHud extends DoubleTextHudElement {
    public FpsHud() {
        super("fps", "Displays your FPS.", "FPS: ");
    }

    @Override
    protected String getRight() {
        return Integer.toString(((MinecraftClientAccessor) mc).getFps());
    }
}
