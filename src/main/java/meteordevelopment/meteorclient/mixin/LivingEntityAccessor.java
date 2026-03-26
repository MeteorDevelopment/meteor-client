/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Invoker("jumpInLiquid")
    void meteor$swimUpwards(TagKey<Fluid> fluid);

    @Accessor("jumping")
    boolean meteor$isJumping();

    @Accessor("noJumpDelay")
    int meteor$getJumpCooldown();

    @Accessor("noJumpDelay")
    void meteor$setJumpCooldown(int cooldown);
}
