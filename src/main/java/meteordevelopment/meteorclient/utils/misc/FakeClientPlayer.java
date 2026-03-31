/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.PreInit;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.world.Difficulty;

import java.util.UUID;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class FakeClientPlayer {
    private static ClientLevel world;
    private static RemotePlayer player;
    private static PlayerInfo playerListEntry;

    private static UUID lastId;
    private static boolean needsNewEntry;

    private FakeClientPlayer() {
    }

    @PreInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(FakeClientPlayer.class);
    }

    public static RemotePlayer getPlayer() {
        UUID id = mc.getSession().getUuidOrNull();

        if (player == null || (!id.equals(lastId))) {
            if (world == null) {
                world = new ClientWorld(
                    new ClientPlayNetworkHandler(mc, new ClientConnection(PacketFlow.CLIENTBOUND), new ClientConnectionState(
                        null,
                        new GameProfile(mc.getSession().getUuidOrNull(), mc.getSession().getUsername()),
                        null,
                        null,
                        null,
                        null,
                        mc.getCurrentServerEntry(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false)
                    ),
                    new ClientLevel.Properties(Difficulty.NORMAL, false, false),
                    world.getRegistryKey(),
                    world.getDimensionEntry(),
                    1,
                    1,
                    null,
                    false,
                    0,
                    world.getSeaLevel()
                );
            }

            player = new OtherClientPlayerEntity(world, new GameProfile(id, mc.getSession().getUsername()));

            lastId = id;
            needsNewEntry = true;
        }

        return player;
    }

    public static PlayerInfo getPlayerListEntry() {
        if (playerListEntry == null || needsNewEntry) {
            playerListEntry = new PlayerListEntry(new GameProfile(lastId, mc.getSession().getUsername()), false);
            needsNewEntry = false;
        }

        return playerListEntry;
    }
}
