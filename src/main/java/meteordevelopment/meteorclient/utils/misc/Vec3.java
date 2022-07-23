/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

public class Vec3 {
    public double x, y, z;

    public Vec3() {}

    public Vec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3 set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;

        return this;
    }

    public Vec3 set(Vec3 vec) {
        x = vec.x;
        y = vec.y;
        z = vec.z;

        return this;
    }

    public Vec3 set(Vec3d vec) {
        x = vec.x;
        y = vec.y;
        z = vec.z;

        return this;
    }

    public Vec3 set(Entity entity, double tickDelta) {
        x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
        y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
        z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());

        return this;
    }

    public Vec3 add(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;

        return this;
    }

    public Vec3 add(Vec3 vec) {
        return add(vec.x, vec.y, vec.z);
    }

    public Vec3 subtract(double x, double y, double z) {
        this.x -= x;
        this.y -= y;
        this.z -= z;

        return this;
    }

    public Vec3 subtract(Vec3d vec) {
        return subtract(vec.x, vec.y, vec.z);
    }

    public Vec3 multiply(double x, double y, double z) {
        this.x *= x;
        this.y *= y;
        this.z *= z;

        return this;
    }

    public Vec3 multiply(double v) {
        return multiply(v, v, v);
    }

    public Vec3 divide(double v) {
        x /= v;
        y /= v;
        z /= v;

        return this;
    }

    public void negate() {
        x = -x;
        y = -y;
        z = -z;
    }

    public double distanceTo(Vec3 vec) {
        double d = vec.x - x;
        double e = vec.y - y;
        double f = vec.z - z;

        return Math.sqrt(d * d + e * e + f * f);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public Vec3 normalize() {
        return divide(length());
    }

    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vec3 vec3 = (Vec3) o;
        return Double.compare(vec3.x, x) == 0 && Double.compare(vec3.y, y) == 0 && Double.compare(vec3.z, z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return String.format("[%.3f, %.3f, %.3f]", x, y, z);
    }
}
