/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.joml.Vector3d;

@SuppressWarnings("UnusedReturnValue")
public interface IVec3d {
    Vec3d meteor$set(double x, double y, double z);

    default Vec3d meteor$set(Vec3i vec) {
        return meteor$set(vec.getX(), vec.getY(), vec.getZ());
    }

    default Vec3d meteor$set(Vector3d vec) {
        return meteor$set(vec.x, vec.y, vec.z);
    }

    default Vec3d meteor$set(Vec3d pos) {
        return meteor$set(pos.x, pos.y, pos.z);
    }

    Vec3d meteor$setXZ(double x, double z);

    Vec3d meteor$setY(double y);
}
