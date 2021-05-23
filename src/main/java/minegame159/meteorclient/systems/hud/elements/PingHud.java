/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.hud.elements;

import minegame159.meteorclient.systems.hud.DoubleTextHudElement;
import minegame159.meteorclient.systems.hud.ElementRegister;
import net.minecraft.client.network.PlayerListEntry;

@ElementRegister(name = "ping")
public class PingHud extends DoubleTextHudElement {
    public PingHud() {
        super("ping", "Displays your ping.", "Ping: ");
    }

    @Override
    protected String getRight() {
        if (isInEditor()) return "0";

        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());

        if (playerListEntry != null) return Integer.toString(playerListEntry.getLatency());
        return "0";
    }
}
