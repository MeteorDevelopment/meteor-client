/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import net.minecraft.util.math.BlockPos;

public interface IBox {
    void expand(double v);

    void set(double x1, double y1, double z1, double x2, double y2, double z2);

    default void set(BlockPos pos) {
        set(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }
}
