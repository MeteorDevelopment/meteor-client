package me.jellysquid.mods.lithium.mixin.gen.chunk_region;

import me.jellysquid.mods.lithium.common.util.Pos;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChunkRegion.class)
public abstract class ChunkRegionMixin implements StructureWorldAccess {
    @Shadow
    @Final
    private ChunkPos lowerCorner;

    @Shadow
    @Final
    private int width;

    // Array view of the chunks in the region to avoid an unnecessary de-reference
    private Chunk[] chunksArr;

    // The starting position of this region
    private int minChunkX, minChunkZ;

    /**
     * @author JellySquid
     */
    @Inject(method = "<init>(Lnet/minecraft/server/world/ServerWorld;Ljava/util/List;Lnet/minecraft/world/chunk/ChunkStatus;I)V", at = @At("RETURN"))
    private void init(ServerWorld world, List<Chunk> chunks, ChunkStatus chunkStatus, int i, CallbackInfo ci) {
        this.minChunkX = this.lowerCorner.x;
        this.minChunkZ = this.lowerCorner.z;

        this.chunksArr = chunks.toArray(new Chunk[0]);
    }

    /**
     * @reason Avoid pointer de-referencing, make method easier to inline
     * @author JellySquid
     */
    @Overwrite
    public BlockState getBlockState(BlockPos pos) {
        int x = (Pos.ChunkCoord.fromBlockCoord(pos.getX())) - this.minChunkX;
        int z = (Pos.ChunkCoord.fromBlockCoord(pos.getZ())) - this.minChunkZ;
        int w = this.width;

        if (x >= 0 && z >= 0 && x < w && z < w) {
            return this.chunksArr[x + z * w].getBlockState(pos);
        } else {
            throw new NullPointerException("No chunk exists at " + new ChunkPos(pos));
        }
    }

    /**
     * @reason Use the chunk array for faster access
     * @author SuperCoder7979, 2No2Name
     */
    @Overwrite
    public Chunk getChunk(int chunkX, int chunkZ) {
        int x = chunkX - this.minChunkX;
        int z = chunkZ - this.minChunkZ;
        int w = this.width;

        if (x >= 0 && z >= 0 && x < w && z < w) {
            return this.chunksArr[x + z * w];
        } else {
            throw new NullPointerException("No chunk exists at " + new ChunkPos(chunkX, chunkZ));
        }
    }

    /**
     * Use our chunk fetch function
     */
    public Chunk getChunk(BlockPos pos) {
        // Skip checking chunk.getStatus().isAtLeast(ChunkStatus.EMPTY) here, because it is always true
        return this.getChunk(Pos.ChunkCoord.fromBlockCoord(pos.getX()), Pos.ChunkCoord.fromBlockCoord(pos.getZ()));
    }
}
