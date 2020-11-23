/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixininterface;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public interface IVec3d {
    void set(double x, double y, double z);

    default void set(Vec3d vec) {
        set(vec.x, vec.y, vec.z);
    }

    default void set(Vec3i vec) {
        set(vec.getX(), vec.getY(), vec.getZ());
    }

    void setY(double y);
}
