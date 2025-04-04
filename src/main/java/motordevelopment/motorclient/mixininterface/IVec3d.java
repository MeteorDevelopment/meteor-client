/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixininterface;

import net.minecraft.util.math.Vec3i;
import org.joml.Vector3d;

public interface IVec3d {
    void motor$set(double x, double y, double z);

    default void motor$set(Vec3i vec) {
        motor$set(vec.getX(), vec.getY(), vec.getZ());
    }

    default void motor$set(Vector3d vec) {
        motor$set(vec.x, vec.y, vec.z);
    }

    void motor$setXZ(double x, double z);

    void motor$setY(double y);
}
