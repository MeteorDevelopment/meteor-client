package me.jellysquid.mods.lithium.mixin.math.fast_blockpos;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Inlining the offsets makes a *very small* improvement to the generated machine code, but when paired
 * with the other patches in Direction, it seems worthwhile enough given how simple the implementation is...
 *
 * This class has been subject of many similar patches, but I've not been able to replicate the same magnitude of
 * improvement that has apparently been shown by other people through just inlining direction vectors.
 *
 * In fact, when inlining the {@link BlockPos#offset(Direction)} method, the common approach ended up generating
 * significantly worse machine code by introducing a costly branching switch table and hugely inflating the
 * instruction count, which seems to match what I would expect.
 */
@Mixin(BlockPos.class)
public abstract class BlockPosMixin extends Vec3i {

    public BlockPosMixin(int x, int y, int z) {
        super(x, y, z);
    }

    /**
     * @author JellySquid
     * @reason Simplify and inline
     */
    @Override
    @Overwrite
    public BlockPos up() {
        return new BlockPos(this.getX(), this.getY() + 1, this.getZ());
    }

    /**
     * @author JellySquid
     * @reason Simplify and inline
     */
    @Override
    @Overwrite
    public BlockPos up(int distance) {
        return new BlockPos(this.getX(), this.getY() + distance, this.getZ());
    }

    /**
     * @author JellySquid
     * @reason Simplify and inline
     */
    @Override
    @Overwrite
    public BlockPos down() {
        return new BlockPos(this.getX(), this.getY() - 1, this.getZ());
    }

    /**
     * @author JellySquid
     * @reason Simplify and inline
     */
    @Override
    @Overwrite
    public BlockPos down(int distance) {
        return new BlockPos(this.getX(), this.getY() - distance, this.getZ());
    }

    /**
     * @author JellySquid
     * @reason Simplify and inline
     */
    @Overwrite
    public BlockPos north() {
        return new BlockPos(this.getX(), this.getY(), this.getZ() - 1);
    }

    /**
     * @author JellySquid
     * @reason Simplify and inline
     */
    @Overwrite
    public BlockPos north(int distance) {
        return new BlockPos(this.getX(), this.getY(), this.getZ() - distance);
    }

    /**
     * @author JellySquid
     * @reason Simplify and inline
     */
    @Overwrite
    public BlockPos south() {
        return new BlockPos(this.getX(), this.getY(), this.getZ() + 1);
    }

    /**
     * @author JellySquid
     * @reason Simplify and inline
     */
    @Overwrite
    public BlockPos south(int distance) {
        return new BlockPos(this.getX(), this.getY(), this.getZ() + distance);
    }

    /**
     * @author JellySquid
     * @reason Simplify and inline
     */
    @Overwrite
    public BlockPos west() {
        return new BlockPos(this.getX() - 1, this.getY(), this.getZ());
    }

    /**
     * @author JellySquid
     * @reason Simplify and inline
     */
    @Overwrite
    public BlockPos west(int distance) {
        return new BlockPos(this.getX() - distance, this.getY(), this.getZ());
    }

    /**
     * @author JellySquid
     * @reason Simplify and inline
     */
    @Overwrite
    public BlockPos east() {
        return new BlockPos(this.getX() + 1, this.getY(), this.getZ());
    }

    /**
     * @author JellySquid
     * @reason Simplify and inline
     */
    @Overwrite
    public BlockPos east(int distance) {
        return new BlockPos(this.getX() + distance, this.getY(), this.getZ());
    }
}
