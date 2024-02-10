package me.jellysquid.mods.lithium.mixin.util.block_entity_retrieval;

import me.jellysquid.mods.lithium.common.world.blockentity.BlockEntityGetter;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(World.class)
public abstract class WorldMixin implements BlockEntityGetter, WorldAccess {
    @Shadow
    @Final
    public boolean isClient;

    @Shadow
    @Final
    private Thread thread;

    @Shadow
    public abstract WorldChunk getChunk(int i, int j);

    @Shadow
    @Nullable
    public abstract Chunk getChunk(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create);

    @Override
    public BlockEntity getLoadedExistingBlockEntity(BlockPos pos) {
        if (!this.isOutOfHeightLimit(pos)) {
            if (this.isClient || Thread.currentThread() == this.thread) {
                Chunk chunk = this.getChunk(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()), ChunkStatus.FULL, false);
                if (chunk != null) {
                    return chunk.getBlockEntity(pos);
                }
            }
        }
        return null;
    }
}
