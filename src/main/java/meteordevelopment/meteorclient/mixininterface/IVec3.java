/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

@SuppressWarnings("UnusedReturnValue")
public interface IVec3 {
    Vec3 meteor$set(double x, double y, double z);

    default Vec3 meteor$set(Vec3i vec) {
        return meteor$set(vec.getX(), vec.getY(), vec.getZ());
    }

    default Vec3 meteor$set(Vector3d vec) {
        return meteor$set(vec.x, vec.y, vec.z);
    }

    default Vec3 meteor$set(Vec3 pos) {
        return meteor$set(pos.x, pos.y, pos.z);
    }

    Vec3 meteor$setXZ(double x, double z);

    Vec3 meteor$setY(double y);
}
