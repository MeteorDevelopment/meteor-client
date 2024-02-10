package me.jellysquid.mods.lithium.common.util.math;

import net.minecraft.util.math.Vec3d;

public class MutableVec3d {
    private double x, y, z;

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public void add(Vec3d vec) {
        this.x += vec.x;
        this.y += vec.y;
        this.z += vec.z;
    }

    public Vec3d toImmutable() {
        return new Vec3d(this.x, this.y, this.z);
    }
}

