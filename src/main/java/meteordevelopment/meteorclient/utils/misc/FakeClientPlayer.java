/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class FakeClientPlayer {
    private static PlayerEntity player;
    private static PlayerListEntry playerListEntry;

    private static String lastId;
    private static boolean needsNewEntry;

    public static PlayerEntity getPlayer() {
        String id = mc.getSession().getUuid();

        if (player == null || (!id.equals(lastId))) {
            player = new OtherClientPlayerEntity(mc.world, mc.getSession().getProfile());
            lastId = id;
            needsNewEntry = true;
        }

        return player;
    }

    public static PlayerListEntry getPlayerListEntry() {
        if (playerListEntry == null || needsNewEntry) {
            playerListEntry = new PlayerListEntry(mc.getSession().getProfile(), false);
            needsNewEntry = false;
        }

        return playerListEntry;
    }
}
