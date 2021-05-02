/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.entity.fakeplayer;

import java.util.ArrayList;
import java.util.List;

public class FakePlayerManager {
    private static final List<FakePlayerEntity> fakePlayers = new ArrayList<>();

    public static void add(String name, float health, boolean copyInv) {
        FakePlayerEntity fakePlayer = new FakePlayerEntity(name, health, copyInv);
        fakePlayers.add(fakePlayer);
    }

    public static void clear() {
        for (FakePlayerEntity fakePlayer : fakePlayers) {
            fakePlayers.remove(fakePlayer);
            fakePlayer.despawn();
        }
    }

    public static List<FakePlayerEntity> getPlayers() {
        return fakePlayers;
    }

    public static int size() {
        return fakePlayers.size();
    }
}
