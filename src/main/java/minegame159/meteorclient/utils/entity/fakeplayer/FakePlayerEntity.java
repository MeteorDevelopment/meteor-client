/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.entity.fakeplayer;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;

import static minegame159.meteorclient.utils.Utils.mc;

public class FakePlayerEntity extends OtherClientPlayerEntity {

    public FakePlayerEntity(String name, float health, boolean copyInv) {
        super(mc.world, new GameProfile(mc.player.getUuid(), name));

        copyPositionAndRotation(mc.player);

        headYaw = mc.player.headYaw;
        bodyYaw = mc.player.bodyYaw;

        Byte playerModel = mc.player.getDataTracker().get(PlayerEntity.PLAYER_MODEL_PARTS);
        dataTracker.set(PlayerEntity.PLAYER_MODEL_PARTS, playerModel);

        getAttributes().setFrom(mc.player.getAttributes());

        capeX = getX();
        capeY = getY();
        capeZ = getZ();

        if (health <= 20) {
            setHealth(health);
        } else {
            setHealth(health);
            setAbsorptionAmount(health - 20);
        }

        if (copyInv) inventory.clone(mc.player.inventory);

        spawn();
    }
    private void spawn() {
        removed = false;
        mc.world.addEntity(getEntityId(), this);
    }

    public void despawn() {
        mc.world.removeEntity(getEntityId());
        removed = true;
    }
}