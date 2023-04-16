/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import meteordevelopment.meteorclient.utils.PreInit;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class FakeClientPlayer {
    private static ClientWorld world;
    private static PlayerEntity player;

    private static String lastId;
    private static Unsafe unsafe;

    @PreInit
    public static void init() {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafe = (Unsafe) unsafeField.get(null);
        } catch (Exception ignored) {
        }
    }

    public static PlayerEntity getPlayer() {
        String id = mc.getSession().getUuid();

        try {
            if (world == null) world = (ClientWorld) unsafe.allocateInstance(ClientWorld.class);
        } catch (InstantiationException ignored) {
        }

        if (player == null || (!id.equals(lastId))) {
            player = new OtherClientPlayerEntity(world, mc.getSession().getProfile());
            lastId = id;
        }

        return player;
    }
}
