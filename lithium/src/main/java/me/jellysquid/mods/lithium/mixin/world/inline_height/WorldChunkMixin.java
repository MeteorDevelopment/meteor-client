package me.jellysquid.mods.lithium.mixin.world.inline_height;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin implements HeightLimitView {

    @Shadow
    @Final
    World world;

    @Override
    public int getTopY() {
        return this.world.getTopY();
    }

    @Override
    public int countVerticalSections() {
        return this.world.countVerticalSections();
    }

    @Override
    public int getBottomSectionCoord() {
        return this.world.getBottomSectionCoord();
    }

    @Override
    public int getTopSectionCoord() {
        return this.world.getTopSectionCoord();
    }

    @Override
    public boolean isOutOfHeightLimit(BlockPos pos) {
        return this.world.isOutOfHeightLimit(pos);
    }

    @Override
    public boolean isOutOfHeightLimit(int y) {
        return this.world.isOutOfHeightLimit(y);
    }

    @Override
    public int getSectionIndex(int y) {
        return this.world.getSectionIndex(y);
    }

    @Override
    public int sectionCoordToIndex(int coord) {
        return this.world.sectionCoordToIndex(coord);
    }

    @Override
    public int sectionIndexToCoord(int index) {
        return this.world.sectionIndexToCoord(index);
    }
}
