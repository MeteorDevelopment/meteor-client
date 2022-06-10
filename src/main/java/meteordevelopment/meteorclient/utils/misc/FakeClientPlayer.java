/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.utils.Init;
import meteordevelopment.meteorclient.utils.InitStage;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.text.Text;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class FakeClientPlayer {
    private static ClientWorld world;
    private static PlayerEntity player;
    private static PlayerListEntry playerListEntry;

    private static String lastId;
    private static boolean needsNewEntry;

    @Init(stage = InitStage.Pre)
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(FakeClientPlayer.class);
    }

    @EventHandler
    private static void onGameJoined(GameJoinedEvent event) {
        world = new ClientWorld(new ClientPlayNetworkHandler(mc, null, new ClientConnection(NetworkSide.CLIENTBOUND), mc.getSession().getProfile(), null), new ClientWorld.Properties(Difficulty.NORMAL, false, false), World.OVERWORLD, RegistryEntry.of(BuiltinRegistries.DIMENSION_TYPE.get(DimensionTypes.OVERWORLD)), 1, 1, mc::getProfiler, null, false, 0);
    }

    public static PlayerEntity getPlayer() {
        String id = mc.getSession().getUuid();

        if (player == null || (!id.equals(lastId))) {
            player = new OtherClientPlayerEntity(world, mc.getSession().getProfile(), mc.player.getPublicKey());

            lastId = id;
            needsNewEntry = true;
        }

        return player;
    }

    public static PlayerListEntry getPlayerListEntry() {
        if (playerListEntry == null || needsNewEntry) {
            playerListEntry = new PlayerListEntry(PlayerListEntryFactory.create(mc.getSession().getProfile(), 0, GameMode.SURVIVAL, Text.of(mc.getSession().getProfile().getName()), player.getPublicKey().data()), mc.getServicesSignatureVerifier());
            needsNewEntry = false;
        }

        return playerListEntry;
    }
}
