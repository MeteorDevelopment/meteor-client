package me.jellysquid.mods.lithium.mixin.alloc.chunk_random;

import me.jellysquid.mods.lithium.common.world.ChunkRandomSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(World.class)
public class WorldMixin implements ChunkRandomSource {
    @Shadow
    protected int lcgBlockSeed;

    /**
     * {@inheritDoc}
     */
    @Override
    public void getRandomPosInChunk(int x, int y, int z, int mask, BlockPos.Mutable out) {
        this.lcgBlockSeed = this.lcgBlockSeed * 3 + 1013904223;
        int rand = this.lcgBlockSeed >> 2;
        out.set(x + (rand & 15), y + (rand >> 16 & mask), z + (rand >> 8 & 15));
    }
}
