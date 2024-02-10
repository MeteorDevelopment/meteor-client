package me.jellysquid.mods.lithium.mixin.experimental.entity.block_caching.fire_lava_touching;

import me.jellysquid.mods.lithium.common.entity.block_tracking.BlockCache;
import me.jellysquid.mods.lithium.common.entity.block_tracking.BlockCacheProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;
import java.util.stream.Stream;

@Mixin(Entity.class)
public abstract class EntityMixin implements BlockCacheProvider {
    private static final Stream<BlockState> EMPTY_BLOCKSTATE_STREAM = Stream.empty();
    @Shadow
    private int fireTicks;

    @Shadow
    protected abstract int getBurningDuration();

    @Shadow
    public boolean wasOnFire;

    @Shadow
    public boolean inPowderSnow;

    @Shadow
    public abstract boolean isWet();

    @Redirect(
            method = "move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getStatesInBoxIfLoaded(Lnet/minecraft/util/math/Box;)Ljava/util/stream/Stream;"
            )
    )
    private Stream<BlockState> skipFireTestIfResultDoesNotMatterOrIsCached(World world, Box box) {
        // Skip scanning the blocks around the entity touches by returning null when the result does not matter
        // Return null when there is no fire / lava => the branch of noneMatch is not taken
        // Otherwise return anything non-null. Here: Stream.empty. See skipNullStream(...) below.
        // Passing null vs Stream.empty() isn't nice but necessary to avoid the slow Stream API. Also
        // [VanillaCopy] the fire / lava check and the side effects (this.fireTicks) and their conditions needed to be copied. This might affect compatibility with other mods.
        if ((this.fireTicks > 0 || this.fireTicks == -this.getBurningDuration()) && (!this.wasOnFire || !this.inPowderSnow && !this.isWet())) {
            return null;
        }


        BlockCache bc = this.getUpdatedBlockCache((Entity)(Object)this);

        byte cachedTouchingFireLava = bc.getIsTouchingFireLava();
        if (cachedTouchingFireLava == (byte) 0) {
            return null;
        } else if (cachedTouchingFireLava == (byte) 1) {
            return EMPTY_BLOCKSTATE_STREAM;
        }

        int minX = MathHelper.floor(box.minX);
        int maxX = MathHelper.floor(box.maxX);
        int minY = MathHelper.floor(box.minY);
        int maxY = MathHelper.floor(box.maxY);
        int minZ = MathHelper.floor(box.minZ);
        int maxZ = MathHelper.floor(box.maxZ);

        if (maxY >= world.getBottomY() && minY < world.getTopY()) {
            if (world.isRegionLoaded(minX, minZ, maxX, maxZ)) {
                BlockPos.Mutable blockPos = new BlockPos.Mutable();
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        for (int x = minX; x <= maxX; x++) {
                            blockPos.set(x, y, z);
                            BlockState state = world.getBlockState(blockPos);
                            if (state.isIn(BlockTags.FIRE) || state.isOf(Blocks.LAVA)) {
                                bc.setCachedTouchingFireLava(true);
                                return EMPTY_BLOCKSTATE_STREAM;
                            }
                        }
                    }
                }
            }
        }
        bc.setCachedTouchingFireLava(false);
        return null;
    }

    @Redirect(
            method = "move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/stream/Stream;noneMatch(Ljava/util/function/Predicate;)Z"
            )
    )
    private boolean skipNullStream(Stream<BlockState> stream, Predicate<BlockState> predicate) {
        return stream == null;
    }
}
