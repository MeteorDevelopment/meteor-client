package me.jellysquid.mods.lithium.mixin.experimental.entity.block_caching.suffocation;

import me.jellysquid.mods.lithium.common.entity.block_tracking.BlockCache;
import me.jellysquid.mods.lithium.common.entity.block_tracking.BlockCacheProvider;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Entity.class)
public abstract class EntityMixin implements BlockCacheProvider {

    @Shadow
    public World world;

    @Shadow
    private EntityDimensions dimensions;

    protected EntityMixin(EntityDimensions dimensions) {
        this.dimensions = dimensions;
    }

    /**
     * @author 2No2Name
     * @reason Avoid stream code, use optimized chunk section iteration order
     */
    @Inject(
            method = "isInsideWall", cancellable = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/BlockPos;stream(Lnet/minecraft/util/math/Box;)Ljava/util/stream/Stream;",
                    shift = At.Shift.BEFORE
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    public void isInsideWall(CallbackInfoReturnable<Boolean> cir, float f, Box box) {
        // [VanillaCopy]
        int minX = MathHelper.floor(box.minX);
        int minY = MathHelper.floor(box.minY);
        int minZ = MathHelper.floor(box.minZ);
        int maxX = MathHelper.floor(box.maxX);
        int maxY = MathHelper.floor(box.maxY);
        int maxZ = MathHelper.floor(box.maxZ);

        BlockCache bc = this.getUpdatedBlockCache((Entity) (Object) this);

        byte cachedSuffocation = bc.getIsSuffocating();
        if (cachedSuffocation == (byte) 0) {
            cir.setReturnValue(false);
            return;
        } else if (cachedSuffocation == (byte) 1) {
            cir.setReturnValue(true);
            return;
        }

        World world = this.world;
        //skip getting blocks when the entity is outside the world height
        //also avoids infinite loop with entities below y = Integer.MIN_VALUE (some modded servers do that)
        if (world.getBottomY() > maxY || world.getTopY() < minY) {
            bc.setCachedIsSuffocating(false);
            cir.setReturnValue(false);
            return;
        }

        BlockPos.Mutable blockPos = new BlockPos.Mutable();
        VoxelShape suffocationShape = null;

        boolean shouldCache = true;

        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    blockPos.set(x, y, z);
                    BlockState blockState = world.getBlockState(blockPos);
                    if (!blockState.isAir() && blockState.shouldSuffocate(this.world, blockPos)) {
                        //We must never cache suffocation with shulker boxes, as they can change suffocation behavior without block state changes and the block listening system therefore does not detect these changes.
                        if (shouldCache && blockState.isIn(BlockTags.SHULKER_BOXES)) {
                            shouldCache = false;
                        }

                        if (suffocationShape == null) {
                            suffocationShape = VoxelShapes.cuboid(new Box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ));
                        }
                        if (VoxelShapes.matchesAnywhere(blockState.getCollisionShape(this.world, blockPos).
                                        offset(blockPos.getX(), blockPos.getY(), blockPos.getZ()),
                                suffocationShape, BooleanBiFunction.AND)) {
                            if (shouldCache) {
                                bc.setCachedIsSuffocating(true);
                            }
                            cir.setReturnValue(true);
                            return;
                        }
                    }
                }
            }
        }
        if (shouldCache) {
            bc.setCachedIsSuffocating(false);
        }
        cir.setReturnValue(false);
    }
}
