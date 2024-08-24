/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.PreInit;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.world.Difficulty;

import java.util.UUID;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class FakeClientPlayer {
    private static ClientWorld world;
    private static PlayerEntity player;
    private static PlayerListEntry playerListEntry;

    private static UUID lastId;
    private static boolean needsNewEntry;

    private FakeClientPlayer() {
    }

    @PreInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(FakeClientPlayer.class);
    }

    public static PlayerEntity getPlayer() {
        UUID id = mc.getSession().getUuidOrNull();

        if (player == null || (!id.equals(lastId))) {
            if (world == null) {
                world = new ClientWorld(
                    new ClientPlayNetworkHandler(mc, new ClientConnection(NetworkSide.CLIENTBOUND), new ClientConnectionState(new GameProfile(mc.getSession().getUuidOrNull(), mc.getSession().getUsername()), null, null, null, null, mc.getCurrentServerEntry(), null, null, null, false, null, null)),
                    new ClientWorld.Properties(Difficulty.NORMAL, false, false),
                    world.getRegistryKey(),
                    world.getDimensionEntry(),
                    1,
                    1,
                    mc::getProfiler,
                    null,
                    false,
                    0
                );
            }

            player = new OtherClientPlayerEntity(world, new GameProfile(id, mc.getSession().getUsername()));

            lastId = id;
            needsNewEntry = true;
        }

        return player;
    }

    public static PlayerListEntry getPlayerListEntry() {
        if (playerListEntry == null || needsNewEntry) {
            playerListEntry = new PlayerListEntry(new GameProfile(lastId, mc.getSession().getUsername()), false);
            needsNewEntry = false;
        }

        return playerListEntry;
    }
}
