/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.misc;

import net.minecraft.entity.Entity;
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
        x = entity.getX() + (entity.getX() - entity.prevX) * tickDelta;
        y = entity.getY() + (entity.getY() - entity.prevY) * tickDelta;
        z = entity.getZ() + (entity.getZ() - entity.prevZ) * tickDelta;
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
