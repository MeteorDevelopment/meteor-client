package me.jellysquid.mods.lithium.mixin.block.moving_block_shapes;

import me.jellysquid.mods.lithium.common.shapes.OffsetVoxelShapeCache;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.PistonHeadBlock;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;


/**
 * @author 2No2Name
 */
@Mixin(PistonBlockEntity.class)
public abstract class PistonBlockEntityMixin {
    private static final VoxelShape[] PISTON_BASE_WITH_MOVING_HEAD_SHAPES = precomputePistonBaseWithMovingHeadShapes();

    @Shadow
    private Direction facing;
    @Shadow
    private boolean extending;
    @Shadow
    private boolean source;


    @Shadow
    private BlockState pushedBlock;

    /**
     * Avoid calling {@link VoxelShapes#union(VoxelShape, VoxelShape)} whenever possible - use precomputed merged piston head + base shapes and
     * cache the results for all union calls with an empty shape as first argument. (these are all other cases)
     */
    @Inject(
            method = "getCollisionShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/shape/VoxelShape;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/Direction;getOffsetX()I",
                    shift = At.Shift.BEFORE
            ),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true
    )
    private void skipVoxelShapeUnion(BlockView world, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir, VoxelShape voxelShape, Direction direction, BlockState blockState, float f) {
        if (this.extending || !this.source || !(this.pushedBlock.getBlock() instanceof PistonBlock)) {
            //here voxelShape2.isEmpty() is guaranteed, vanilla code would call union() which calls simplify()
            VoxelShape blockShape = blockState.getCollisionShape(world, pos);

            //we cache the simplified shapes, as the simplify() method costs a lot of CPU time and allocates several objects
            VoxelShape offsetAndSimplified = getOffsetAndSimplified(blockShape, Math.abs(f), f < 0f ? this.facing.getOpposite() : this.facing);
            cir.setReturnValue(offsetAndSimplified);
        } else {
            //retracting piston heads have to act like their base as well, as the base block is replaced with the moving block
            //f >= 0f is guaranteed (assuming no other mod interferes)
            int index = getIndexForMergedShape(f, this.facing);
            cir.setReturnValue(PISTON_BASE_WITH_MOVING_HEAD_SHAPES[index]);
        }
    }

    /**
     * We cache the offset and simplified VoxelShapes that are otherwise constructed on every call of getCollisionShape.
     * For each offset direction and distance (6 directions, 2 distances each, and no direction with 0 distance) we
     * store the offset and simplified VoxelShapes in the original VoxelShape when they are accessed the first time.
     * We use safe publication, because both the Render and Server thread are using the cache.
     *
     * @param blockShape the original shape, must not be modified after passing it as an argument to this method
     * @param offset     the offset distance
     * @param direction  the offset direction
     * @return blockShape offset and simplified
     */
    private static VoxelShape getOffsetAndSimplified(VoxelShape blockShape, float offset, Direction direction) {
        VoxelShape offsetSimplifiedShape = ((OffsetVoxelShapeCache) blockShape).getOffsetSimplifiedShape(offset, direction);
        if (offsetSimplifiedShape == null) {
            //create the offset shape and store it for later use
            offsetSimplifiedShape = blockShape.offset(direction.getOffsetX() * offset, direction.getOffsetY() * offset, direction.getOffsetZ() * offset).simplify();
            ((OffsetVoxelShapeCache) blockShape).setShape(offset, direction, offsetSimplifiedShape);
        }
        return offsetSimplifiedShape;
    }

    /**
     * Precompute all 18 possible configurations for the merged piston base and head shape.
     *
     * @return The array of the merged VoxelShapes, indexed by {@link PistonBlockEntityMixin#getIndexForMergedShape(float, Direction)}
     */
    private static VoxelShape[] precomputePistonBaseWithMovingHeadShapes() {
        float[] offsets = {0f, 0.5f, 1f};
        Direction[] directions = Direction.values();

        VoxelShape[] mergedShapes = new VoxelShape[offsets.length * directions.length];

        for (Direction facing : directions) {
            VoxelShape baseShape = Blocks.PISTON.getDefaultState().with(PistonBlock.EXTENDED, true)
                    .with(PistonBlock.FACING, facing).getCollisionShape(null, null);
            for (float offset : offsets) {
                //this cache is only required for the merged piston head + base shape.
                //this shape is only used when !this.extending
                //here: isShort = this.extending != 1.0F - this.progress < 0.25F can be simplified to:
                //isShort = f < 0.25F , because f = getAmountExtended(this.progress) can be simplified to f == 1.0F - this.progress
                //therefore isShort is dependent on the offset:
                boolean isShort = offset < 0.25f;

                VoxelShape headShape = (Blocks.PISTON_HEAD.getDefaultState().with(PistonHeadBlock.FACING, facing))
                        .with(PistonHeadBlock.SHORT, isShort).getCollisionShape(null, null);

                VoxelShape offsetHead = headShape.offset(facing.getOffsetX() * offset,
                        facing.getOffsetY() * offset,
                        facing.getOffsetZ() * offset);
                mergedShapes[getIndexForMergedShape(offset, facing)] = VoxelShapes.union(baseShape, offsetHead);
            }

        }

        return mergedShapes;
    }

    private static int getIndexForMergedShape(float offset, Direction direction) {
        if (offset != 0f && offset != 0.5f && offset != 1f) {
            return -1;
        }
        //shape of offset 0 is still dependent on the direction, due to piston head and base being directional blocks
        return (int) (2 * offset) + (3 * direction.getId());
    }
}
