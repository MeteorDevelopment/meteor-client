/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixininterface;

import minegame159.meteorclient.utils.misc.Vec4;
import net.minecraft.util.math.Vec3d;

public interface IMatrix4f {
    void multiplyMatrix(Vec4 v, Vec4 out);

    Vec3d mul(Vec3d vec);
}
