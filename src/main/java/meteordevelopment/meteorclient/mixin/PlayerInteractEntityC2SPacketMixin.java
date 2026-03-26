/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.NoSlow;
import meteordevelopment.meteorclient.systems.modules.movement.Sneak;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(ServerboundInteractPacket.class)
public abstract class PlayerInteractEntityC2SPacketMixin implements IPlayerInteractEntityC2SPacket {
    @Shadow public abstract int entityId();
    @Shadow public abstract net.minecraft.world.phys.Vec3 location();

    @Override
    public boolean meteor$isAttack() {
        return false;
    }

    @Override
    public boolean meteor$isInteractAt() {
        return location() != null;
    }

    @Override
    public Entity meteor$getEntity() {
        return mc.level.getEntity(entityId());
    }

    @ModifyVariable(method = "<init>", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private static boolean setSneaking(boolean usingSecondaryAction) {
        return Modules.get().get(Sneak.class).doPacket() || Modules.get().get(NoSlow.class).airStrict() || usingSecondaryAction;
    }
}
