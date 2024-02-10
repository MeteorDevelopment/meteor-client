package me.jellysquid.mods.lithium.mixin.alloc.enum_values.piston_handler;

import me.jellysquid.mods.lithium.common.util.DirectionConstants;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PistonHandler.class)
public class PistonHandlerMixin {

    @Redirect(
            method = "tryMoveAdjacentBlock(Lnet/minecraft/util/math/BlockPos;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/Direction;values()[Lnet/minecraft/util/math/Direction;"
            )
    )
    private Direction[] removeAllocation() {
        return DirectionConstants.ALL;
    }
}
