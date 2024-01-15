/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Invoker("swimUpward")
    void swimUpwards(TagKey<Fluid> fluid);

    @Accessor("jumping")
    boolean isJumping();

    @Accessor("jumpingCooldown")
    int getJumpCooldown();

    @Accessor("jumpingCooldown")
    void setJumpCooldown(int cooldown);

    @Accessor("POTION_SWIRLS_COLOR")
    static TrackedData<Integer> meteor$getPotionSwirlsColor() {
        throw new AssertionError();
    }

    @Accessor("POTION_SWIRLS_AMBIENT")
    static TrackedData<Boolean> meteor$getPotionSwirlsAmbient() {
        throw new AssertionError();
    }
}
