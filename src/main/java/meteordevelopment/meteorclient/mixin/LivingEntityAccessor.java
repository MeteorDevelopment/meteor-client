/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Invoker("swimUpward")
    void meteor$swimUpwards(TagKey<Fluid> fluid);

    @Accessor("jumping")
    boolean meteor$isJumping();

    @Accessor("jumpingCooldown")
    int meteor$getJumpCooldown();

    @Accessor("jumpingCooldown")
    void meteor$setJumpCooldown(int cooldown);
}
