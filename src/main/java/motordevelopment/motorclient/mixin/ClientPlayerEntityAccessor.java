/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerEntity.class)
public interface ClientPlayerEntityAccessor {
    @Accessor("mountJumpStrength")
    void setMountJumpStrength(float strength);

    @Accessor("ticksSinceLastPositionPacketSent")
    void setTicksSinceLastPositionPacketSent(int ticks);

    @Invoker("canSprint")
    boolean invokeCanSprint();
}
