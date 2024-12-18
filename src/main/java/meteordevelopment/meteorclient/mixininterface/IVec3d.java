/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import net.minecraft.util.math.Vec3i;
import org.joml.Vector3d;

public interface IVec3d {
    void meteor$set(double x, double y, double z);

    default void meteor$set(Vec3i vec) {
        meteor$set(vec.getX(), vec.getY(), vec.getZ());
    }

    default void meteor$set(Vector3d vec) {
        meteor$set(vec.x, vec.y, vec.z);
    }

    void meteor$setXZ(double x, double z);

    void meteor$setY(double y);
}
