/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.EntityControl;
import net.minecraft.entity.ItemSteerable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemSteerable.class)
public interface ItemSteerableMixin {
    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ItemSteerable;getSaddledSpeed()F"))
    private float getSaddledSpeed(ItemSteerable itemSteerable) {
        return Modules.get().get(EntityControl.class).getSaddledSpeed(itemSteerable.getSaddledSpeed());
    }
}
