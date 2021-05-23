/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.hud.elements;

import minegame159.meteorclient.systems.hud.DoubleTextHudElement;
import minegame159.meteorclient.systems.hud.ElementRegister;
import minegame159.meteorclient.utils.Utils;

@ElementRegister(name = "server")
public class ServerHud extends DoubleTextHudElement {
    public ServerHud() {
        super("server", "Displays the server you're currently in.", "Server: ");
    }

    @Override
    protected String getRight() {
        if (!Utils.canUpdate()) return "None";

        return Utils.getWorldName();
    }
}



