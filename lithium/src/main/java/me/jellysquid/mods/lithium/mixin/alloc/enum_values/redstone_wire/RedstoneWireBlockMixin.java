package me.jellysquid.mods.lithium.mixin.alloc.enum_values.redstone_wire;

import me.jellysquid.mods.lithium.common.util.DirectionConstants;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RedstoneWireBlock.class)
public class RedstoneWireBlockMixin {

    @Redirect(
            method = "update(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Direction;values()[Lnet/minecraft/util/math/Direction;")
    )
    private Direction[] removeAllocation1() {
        return DirectionConstants.ALL;
    }

    @Redirect(
            method = "updateNeighbors(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Direction;values()[Lnet/minecraft/util/math/Direction;")
    )
    private Direction[] removeAllocation2() {
        return DirectionConstants.ALL;
    }
}
