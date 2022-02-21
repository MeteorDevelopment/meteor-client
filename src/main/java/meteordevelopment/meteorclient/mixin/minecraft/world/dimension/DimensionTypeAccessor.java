/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2022 Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.minecraft.world.dimension;

import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DimensionType.class)
public interface DimensionTypeAccessor {
    @Accessor("OVERWORLD")
    static DimensionType getOverworld() { return null; }
}
