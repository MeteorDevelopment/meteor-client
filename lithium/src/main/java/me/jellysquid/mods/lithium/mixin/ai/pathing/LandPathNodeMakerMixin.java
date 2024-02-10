package me.jellysquid.mods.lithium.mixin.ai.pathing;

import me.jellysquid.mods.lithium.common.ai.pathing.PathNodeCache;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * Determining the type of node offered by a block state is a very slow operation due to the nasty chain of tag,
 * instanceof, and block property checks. Since each blockstate can only map to one type of node, we can create a
 * cache which stores the result of this complicated code path. This provides a significant speed-up in path-finding
 * code and should be relatively safe.
 */
@Mixin(value = LandPathNodeMaker.class, priority = 990)
public abstract class LandPathNodeMakerMixin {
    /**
     * This mixin requires a priority < 1000 due to fabric api using 1000 and we need to inject before them.
     *
     * @reason Use optimized implementation
     * @author JellySquid, 2No2Name
     */
    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(method = "getCommonNodeType",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/BlockView;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;",
                    shift = At.Shift.AFTER
            ),
            cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void getLithiumCachedCommonNodeType(BlockView world, BlockPos pos, CallbackInfoReturnable<PathNodeType> cir, BlockState blockState) {
        PathNodeType type = PathNodeCache.getPathNodeType(blockState);
        if (type != null) {
            cir.setReturnValue(type);
        }
    }

    /**
     * Modify the method to allow it to just return the behavior of a single block instead of scanning its neighbors.
     * This technique might seem odd, but it allows us to be very mod and fabric-api compatible.
     * If the function is called with usual inputs (nodeType != null), it behaves normally.
     * If the function is called with nodeType == null, only the passed position is checked for its neighbor behavior.
     * <p>
     * This allows Lithium to call this function to initialize its caches. It also allows using this function as fallback
     * for dynamic blocks (shulker boxes and fabric-api dynamic definitions)
     *
     * @author 2No2Name
     */
    @Inject(
            method = "getNodeTypeFromNeighbors", locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(
                    value = "INVOKE", shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/util/math/BlockPos$Mutable;set(III)Lnet/minecraft/util/math/BlockPos$Mutable;"
            ),
            cancellable = true
    )
    private static void doNotChangePositionIfLithiumSinglePosCall(BlockView world, BlockPos.Mutable pos, PathNodeType nodeType, CallbackInfoReturnable<PathNodeType> cir, int posX, int posY, int posZ, int dX, int dY, int dZ) {
        if (nodeType == null) {
            if (dX == -1 && dY == -1 && dZ == -1) {
                pos.set(posX, posY, posZ);
            } else {
                cir.setReturnValue(null);
            }
        }
    }

    /**
     * @reason Use optimized implementation which avoids scanning blocks for dangers where possible
     * @author JellySquid, 2No2Name
     */
    @Redirect(method = "getLandNodeType", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/pathing/LandPathNodeMaker;getNodeTypeFromNeighbors(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos$Mutable;Lnet/minecraft/entity/ai/pathing/PathNodeType;)Lnet/minecraft/entity/ai/pathing/PathNodeType;"))
    private static PathNodeType getNodeTypeFromNeighbors(BlockView world, BlockPos.Mutable pos, PathNodeType type) {
        return PathNodeCache.getNodeTypeFromNeighbors(world, pos, type);
    }
}
