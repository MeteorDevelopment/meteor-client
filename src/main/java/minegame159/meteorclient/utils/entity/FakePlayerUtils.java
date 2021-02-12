/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.utils.entity;

import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.player.FakePlayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import java.util.HashMap;
import java.util.Map;

public class FakePlayerUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final Map<FakePlayerEntity, Integer> players = new HashMap<>();
    public static int ID;

    public static void spawnFakePlayer() {
        FakePlayer module = Modules.get().get(FakePlayer.class);

        if (module.isActive()) {
            FakePlayerEntity fakePlayer = new FakePlayerEntity(module.name.get(), module.copyInv.get(), module.glowing.get(), module.health.get());
            players.put(fakePlayer, ID);
            ID++;
        }
    }
    
    public static void removeFakePlayer(int id) {
        if (Modules.get().isActive(FakePlayer.class)) {
            if (players.isEmpty()) return;

            for (Map.Entry<FakePlayerEntity, Integer> player : players.entrySet()) {
                if (player.getValue() == id) {
                    player.getKey().despawn();
                }
            }
        }
    }

    public static void clearFakePlayers() {
        for (Map.Entry<FakePlayerEntity, Integer> player : players.entrySet()) {
            player.getKey().despawn();
        }
        players.clear();
    }

    public static Map<FakePlayerEntity, Integer> getPlayers() {
        return players;
    }

    public static int getID(FakePlayerEntity entity) {
        return players.getOrDefault(entity, 0);
    }

    public static boolean isFakePlayerOutOfRenderDistance(Entity entity) {
        if (entity instanceof FakePlayerEntity) {
            double x = Math.abs(mc.gameRenderer.getCamera().getPos().x - entity.getX());
            double z = Math.abs(mc.gameRenderer.getCamera().getPos().z - entity.getZ());
            double d = (mc.options.viewDistance + 1) * 16;

            return x > d || z > d;
        }

        return false;
    }
}
