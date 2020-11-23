/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IDimensionType;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DimensionType.class)
public class DimensionTypeMixin implements IDimensionType {
    @Shadow @Final protected static DimensionType THE_NETHER;

    @Shadow @Final protected static DimensionType THE_END;

    @Override
    public DimensionType getNether() {
        return THE_NETHER;
    }

    @Override
    public DimensionType getEnd() {
        return THE_END;
    }
}
