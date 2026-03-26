/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.PreInit;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class FakeClientPlayer {
    private static ClientLevel world;
    private static Player player;
    private static PlayerInfo playerListEntry;

    private static UUID lastId;
    private static boolean needsNewEntry;

    private FakeClientPlayer() {
    }

    @PreInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(FakeClientPlayer.class);
    }

    public static Player getPlayer() {
        UUID id = mc.getUser().getProfileId();

        if (player == null || (!id.equals(lastId))) {
            if (world == null) {
                world = new ClientLevel(
                    new ClientPacketListener(mc, new Connection(PacketFlow.CLIENTBOUND), new CommonListenerCookie(
                        null,
                        new GameProfile(mc.getUser().getProfileId(), mc.getUser().getName()),
                        null,
                        null,
                        null,
                        null,
                        mc.getCurrentServer(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false)
                    ),
                    new ClientLevel.ClientLevelData(Difficulty.NORMAL, false, false),
                    world.dimension(),
                    world.dimensionTypeRegistration(),
                    1,
                    1,
                    null,
                    false,
                    0,
                    world.getSeaLevel()
                );
            }

            player = new RemotePlayer(world, new GameProfile(id, mc.getUser().getName()));

            lastId = id;
            needsNewEntry = true;
        }

        return player;
    }

    public static PlayerInfo getPlayerListEntry() {
        if (playerListEntry == null || needsNewEntry) {
            playerListEntry = new PlayerInfo(new GameProfile(lastId, mc.getUser().getName()), false);
            needsNewEntry = false;
        }

        return playerListEntry;
    }
}
