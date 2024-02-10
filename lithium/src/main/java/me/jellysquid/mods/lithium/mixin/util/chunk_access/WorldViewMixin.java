package me.jellysquid.mods.lithium.mixin.util.chunk_access;

import me.jellysquid.mods.lithium.common.world.ChunkView;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(WorldView.class)
public interface WorldViewMixin extends ChunkView {

    @Shadow
    @Nullable Chunk getChunk(int var1, int var2, ChunkStatus var3, boolean var4);

    @Override
    default @Nullable Chunk getLoadedChunk(int chunkX, int chunkZ) {
        return this.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
    }
}
