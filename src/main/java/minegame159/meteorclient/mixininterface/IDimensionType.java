/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixininterface;

import net.minecraft.world.dimension.DimensionType;

public interface IDimensionType {
    DimensionType getNether();

    DimensionType getEnd();
}
