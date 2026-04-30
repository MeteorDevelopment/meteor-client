/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.EntityControl;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractBoat.class)
public abstract class AbstractBoatMixin {
    @ModifyExpressionValue(method = "controlBoat", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/vehicle/boat/AbstractBoat;inputLeft:Z", opcode = Opcodes.GETFIELD))
    private boolean modifyPressingLeft(boolean original) {
        if (Modules.get().isActive(EntityControl.class) && Modules.get().get(EntityControl.class).lockYaw.get())
            return false;
        return original;
    }

    @ModifyExpressionValue(method = "controlBoat", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/vehicle/boat/AbstractBoat;inputRight:Z", opcode = Opcodes.GETFIELD))
    private boolean modifyPressingRight(boolean original) {
        if (Modules.get().isActive(EntityControl.class) && Modules.get().get(EntityControl.class).lockYaw.get())
            return false;
        return original;
    }
}
