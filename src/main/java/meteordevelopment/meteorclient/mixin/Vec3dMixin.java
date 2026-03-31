/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IVec3d;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

// TODO(Ravel): can not resolve target class Vec3d
// TODO(Ravel): can not resolve target class Vec3d
@Mixin(Vec3d.class)
public abstract class Vec3dMixin implements IVec3d {
    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    @Final
    @Mutable
    public double x;
    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    @Final
    @Mutable
    public double y;
    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    @Final
    @Mutable
    public double z;

    @Override
    public Vec3d meteor$set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;

        return (Vec3d) (Object) this;
    }

    @Override
    public Vec3d meteor$setXZ(double x, double z) {
        this.x = x;
        this.z = z;

        return (Vec3d) (Object) this;
    }

    @Override
    public Vec3d meteor$setY(double y) {
        this.y = y;

        return (Vec3d) (Object) this;
    }
}
