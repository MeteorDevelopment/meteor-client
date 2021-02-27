/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;

public class FakePlayerEntity extends OtherClientPlayerEntity {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final ClientPlayerEntity player = mc.player;
    private final ClientWorld world = mc.world;

    public FakePlayerEntity(String name, boolean copyInv, boolean glowing, float health) {
        super(mc.world, new GameProfile(mc.player.getUuid(), name));

        copyPositionAndRotation(player);
        copyPlayerModel(player, this);
        copyRotation();
        copyAttributes();
        resetCapeMovement();
        setHealth(health);

        if (copyInv) inventory.clone(mc.player.inventory);
        if (glowing) setGlowing(true);

        spawn();
    }

    private void copyPlayerModel(Entity from, Entity to) {
        DataTracker fromTracker = from.getDataTracker();
        DataTracker toTracker = to.getDataTracker();
        Byte playerModel = fromTracker.get(PlayerEntity.PLAYER_MODEL_PARTS);
        toTracker.set(PlayerEntity.PLAYER_MODEL_PARTS, playerModel);
    }

    private void copyRotation() {
        headYaw = player.headYaw;
        bodyYaw = player.bodyYaw;
    }

    private void copyAttributes() {
        getAttributes().setFrom(player.getAttributes());
    }

    private void resetCapeMovement() {
        capeX = getX();
        capeY = getY();
        capeZ = getZ();
    }

    private void spawn() {
        world.addEntity(getEntityId(), this);
    }

    public void despawn() {
        removed = true;
    }
}