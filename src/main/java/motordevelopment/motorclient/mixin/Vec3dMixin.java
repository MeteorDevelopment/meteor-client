/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import motordevelopment.motorclient.mixininterface.IVec3d;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Vec3d.class)
public abstract class Vec3dMixin implements IVec3d {
    @Shadow @Final @Mutable public double x;
    @Shadow @Final @Mutable public double y;
    @Shadow @Final @Mutable public double z;

    @Override
    public void motor$set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void motor$setXZ(double x, double z) {
        this.x = x;
        this.z = z;
    }

    @Override
    public void motor$setY(double y) {
        this.y = y;
    }
}
