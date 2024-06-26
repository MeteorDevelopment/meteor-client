/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IItemEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin implements IItemEntity {
    @Unique
    private Vec3d rotation = new Vec3d(0, 0, 0);

    @Override
    public Vec3d getRotation() {
        return rotation;
    }

    @Override
    public void setRotation(Vec3d rotation) {
        this.rotation = rotation;
    }
}
