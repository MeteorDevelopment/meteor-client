/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.ShulkerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShulkerEntity.class)
public interface ShulkerEntityAccessor {
    @Accessor("PEEK_AMOUNT")
    static TrackedData<Byte> meteor$getPeekAmount() {
        throw new AssertionError();
    }

    @Accessor("COVERED_ARMOR_BONUS")
    static EntityAttributeModifier meteor$getCoveredArmorBonus() {
        throw new AssertionError();
    }
}
