/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IServerboundInteractPacket;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.NoSlow;
import meteordevelopment.meteorclient.systems.modules.movement.Sneak;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(ServerboundInteractPacket.class)
public abstract class ServerboundInteractPacketMixin implements IServerboundInteractPacket {
    @Shadow
    @Final
    private ServerboundInteractPacket.Action action;
    @Shadow
    @Final
    private int entityId;

    @Override
    public ServerboundInteractPacket.ActionType meteor$getType() {
        return action.getType();
    }

    @Override
    public Entity meteor$getEntity() {
        return mc.level.getEntity(entityId);
    }

    @ModifyVariable(method = "<init>(IZLnet/minecraft/network/protocol/game/ServerboundInteractPacket$Action;)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private static boolean setSneaking(boolean sneaking) {
        return Modules.get().get(Sneak.class).doPacket() || Modules.get().get(NoSlow.class).airStrict() || sneaking;
    }
}
