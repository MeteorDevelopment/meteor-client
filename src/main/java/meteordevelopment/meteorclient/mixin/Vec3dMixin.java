/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IVec3d;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Vec3d.class)
public class Vec3dMixin implements IVec3d {
    @Shadow @Final @Mutable public double x;
    @Shadow @Final @Mutable public double y;
    @Shadow @Final @Mutable public double z;

    @Override
    public void set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void setXZ(double x, double z) {
        this.x = x;
        this.z = z;
    }

    @Override
    public void setY(double y) {
        this.y = y;
    }
}
