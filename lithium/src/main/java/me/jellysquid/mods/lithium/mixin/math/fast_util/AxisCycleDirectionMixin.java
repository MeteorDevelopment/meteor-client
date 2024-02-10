package me.jellysquid.mods.lithium.mixin.math.fast_util;

import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * The JVM has difficulty optimizing these functions due to the use of dynamic dispatch. They can trivially be
 * implemented as a simple switch lookup table. Switch-on-enum is avoided due to issues in Mixin hotswap.
 */
public class AxisCycleDirectionMixin {
    static {
        assert Direction.Axis.X.ordinal() == 0;
        assert Direction.Axis.Y.ordinal() == 1;
        assert Direction.Axis.Z.ordinal() == 2;
        assert Direction.Axis.values().length == 3;
    }

    @Mixin(targets = "net/minecraft/util/math/AxisCycleDirection$2")
    public static class ForwardMixin {
        /**
         * @reason Avoid expensive array/modulo operations
         * @author JellySquid
         */
        @Overwrite
        public Direction.Axis cycle(Direction.Axis axis) {
            switch (axis.ordinal()) {
                case 0: //X
                    return Direction.Axis.Y;
                case 1: //Y
                    return Direction.Axis.Z;
                case 2: //Z
                    return Direction.Axis.X;
            }

            throw new IllegalArgumentException();
        }
    }

    @Mixin(targets = "net/minecraft/util/math/AxisCycleDirection$3")
    public static class BackwardMixin {
        /**
         * @reason Avoid expensive array/modulo operations
         * @author JellySquid
         */
        @Overwrite
        public Direction.Axis cycle(Direction.Axis axis) {
            switch (axis.ordinal()) {
                case 0: //X
                    return Direction.Axis.Z;
                case 1: //Y
                    return Direction.Axis.X;
                case 2: //Z
                    return Direction.Axis.Y;
            }

            throw new IllegalArgumentException();
        }
    }
}
