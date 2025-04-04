/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixininterface;

import net.minecraft.util.math.BlockPos;

public interface IBox {
    void motor$expand(double v);

    void motor$set(double x1, double y1, double z1, double x2, double y2, double z2);

    default void motor$set(BlockPos pos) {
        motor$set(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }
}
