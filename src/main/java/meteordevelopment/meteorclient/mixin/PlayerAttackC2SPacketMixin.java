/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(ServerboundAttackPacket.class)
public abstract class PlayerAttackC2SPacketMixin implements IPlayerInteractEntityC2SPacket {
    @Shadow public abstract int entityId();

    @Override
    public boolean meteor$isAttack() {
        return true;
    }

    @Override
    public boolean meteor$isInteractAt() {
        return false;
    }

    @Override
    public Entity meteor$getEntity() {
        return mc.level.getEntity(entityId());
    }
}
