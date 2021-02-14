/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.misc;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Vec3 {
    public double x, y, z;

    public void set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void set(Vec3 vec) {
        x = vec.x;
        y = vec.y;
        z = vec.z;
    }

    public void set(Vec3d vec) {
        x = vec.x;
        y = vec.y;
        z = vec.z;
    }

    public void set(Entity entity, double tickDelta) {
        x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
        y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
        z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());
    }

    public void add(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;
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
}
