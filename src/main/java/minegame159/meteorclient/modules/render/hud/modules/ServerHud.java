/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.utils.Utils;

public class ServerHud extends DoubleTextHudElement {
    public ServerHud(HUD hud) {
        super(hud, "server", "Displays the server you're currently.", "Server: ");
    }

    @Override
    protected String getRight() {
        if (mc.world == null) return "Null";

        if (mc.isInSingleplayer()) return "Singleplayer";
        else return Utils.getWorldName();
    }


}



