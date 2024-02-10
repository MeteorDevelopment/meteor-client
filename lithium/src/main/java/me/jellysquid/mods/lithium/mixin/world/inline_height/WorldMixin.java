package me.jellysquid.mods.lithium.mixin.world.inline_height;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/**
 * Implement world height related methods directly instead of going through WorldView and Dimension
 */
@Mixin(World.class)
public abstract class WorldMixin implements HeightLimitView {
    @Shadow
    public abstract DimensionType getDimension();

    private int bottomY;
    private int height;
    private int topYInclusive;

    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    private void initHeightCache(MutableWorldProperties properties, RegistryKey<?> registryRef, DynamicRegistryManager registryManager, RegistryEntry<?> dimensionEntry, Supplier<?> profiler, boolean isClient, boolean debugWorld, long biomeAccess, int maxChainedNeighborUpdates, CallbackInfo ci) {
        this.height = this.getDimension().height();
        this.bottomY = this.getDimension().minY();
        this.topYInclusive = this.bottomY + this.height - 1;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public int getBottomY() {
        return this.bottomY;
    }

    @Override
    public int countVerticalSections() {
        return ((this.topYInclusive >> 4) + 1) - (this.bottomY >> 4);
    }

    @Override
    public int getBottomSectionCoord() {
        return this.bottomY >> 4;
    }

    @Override
    public int getTopSectionCoord() {
        return (this.topYInclusive >> 4) + 1;
    }

    @Override
    public boolean isOutOfHeightLimit(BlockPos pos) {
        int y = pos.getY();
        return (y < this.bottomY) || (y > this.topYInclusive);
    }

    @Override
    public boolean isOutOfHeightLimit(int y) {
        return (y < this.bottomY) || (y > this.topYInclusive);
    }

    @Override
    public int getSectionIndex(int y) {
        return (y >> 4) - (this.bottomY >> 4);
    }

    @Override
    public int sectionCoordToIndex(int coord) {
        return coord - (this.bottomY >> 4);

    }

    @Override
    public int sectionIndexToCoord(int index) {
        return index + (this.bottomY >> 4);
    }

    @Override
    public int getTopY() {
        return this.topYInclusive + 1;
    }
}