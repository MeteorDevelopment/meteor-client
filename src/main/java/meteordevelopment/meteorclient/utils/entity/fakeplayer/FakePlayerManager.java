/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.entity.fakeplayer;

import meteordevelopment.meteorclient.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class FakePlayerManager {
    private static final List<FakePlayerEntity> ENTITIES = new ArrayList<>();

    private FakePlayerManager() {
    }

    public static List<FakePlayerEntity> getFakePlayers() {
        return ENTITIES;
    }

    public static FakePlayerEntity get(String name) {
        for (FakePlayerEntity fp : ENTITIES) {
            if (fp.getName().getString().equals(name)) return fp;
        }

        return null;
    }

    public static void add(String name, float health, boolean copyInv) {
        if (!Utils.canUpdate()) return;

        FakePlayerEntity fakePlayer = new FakePlayerEntity(mc.player, name, health, copyInv);
        fakePlayer.spawn();
        ENTITIES.add(fakePlayer);
    }

    public static void remove(FakePlayerEntity fp) {
        ENTITIES.removeIf(fp1 -> {
            if (fp1.getName().getString().equals(fp.getName().getString())) {
                fp1.despawn();
                return true;
            }

            return false;
        });
    }

    public static void clear() {
        if (ENTITIES.isEmpty()) return;
        ENTITIES.forEach(FakePlayerEntity::despawn);
        ENTITIES.clear();
    }

    public static void forEach(Consumer<FakePlayerEntity> action) {
        for (FakePlayerEntity fakePlayer : ENTITIES) {
            action.accept(fakePlayer);
        }
    }

    public static int count() {
        return ENTITIES.size();
    }

    public static Stream<FakePlayerEntity> stream() {
        return ENTITIES.stream();
    }

    public static boolean contains(FakePlayerEntity fp) {
        return ENTITIES.contains(fp);
    }
}
