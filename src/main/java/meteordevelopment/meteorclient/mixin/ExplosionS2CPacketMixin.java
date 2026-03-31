/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IExplosionS2CPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

// TODO(Ravel): can not resolve target class ExplosionS2CPacket
// TODO(Ravel): can not resolve target class ExplosionS2CPacket
@Mixin(ExplosionS2CPacket.class)
public abstract class ExplosionS2CPacketMixin implements IExplosionS2CPacket {
    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    @Final
    @Mutable
    private Optional<Vec3> playerKnockback;

    @Override
    public void meteor$setVelocityX(float velocity) {
        if (playerKnockback.isPresent()) {
            Vec3 kb = playerKnockback.get();
            playerKnockback = Optional.of(new Vec3d(velocity, kb.y, kb.z));
        } else {
            playerKnockback = Optional.of(new Vec3d(velocity, 0, 0));
        }
    }

    @Override
    public void meteor$setVelocityY(float velocity) {
        if (playerKnockback.isPresent()) {
            Vec3 kb = playerKnockback.get();
            playerKnockback = Optional.of(new Vec3d(kb.x, velocity, kb.z));
        } else {
            playerKnockback = Optional.of(new Vec3d(0, velocity, 0));
        }
    }

    @Override
    public void meteor$setVelocityZ(float velocity) {
        if (playerKnockback.isPresent()) {
            Vec3 kb = playerKnockback.get();
            playerKnockback = Optional.of(new Vec3d(kb.x, kb.y, velocity));
        } else {
            playerKnockback = Optional.of(new Vec3d(0, 0, velocity));
        }
    }
}
