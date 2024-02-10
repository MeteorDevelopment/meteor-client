package me.jellysquid.mods.lithium.mixin.world.combined_heightmap_update;

import me.jellysquid.mods.lithium.common.world.chunk.heightmap.CombinedHeightmapUpdate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.BlendingData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;

@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin extends Chunk {
    public WorldChunkMixin(ChunkPos pos, UpgradeData upgradeData, HeightLimitView heightLimitView, Registry<Biome> biome, long inhabitedTime, @Nullable ChunkSection[] sectionArrayInitializer, @Nullable BlendingData blendingData) {
        super(pos, upgradeData, heightLimitView, biome, inhabitedTime, sectionArrayInitializer, blendingData);
    }

    @Redirect(
            method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;")
    )
    private <K, V> V skipGetHeightmap(Map<K, V> heightmaps, K heightmapType) {
        if (heightmapType == Heightmap.Type.MOTION_BLOCKING || heightmapType == Heightmap.Type.MOTION_BLOCKING_NO_LEAVES || heightmapType == Heightmap.Type.OCEAN_FLOOR || heightmapType == Heightmap.Type.WORLD_SURFACE) {
            return null;
        }
        return heightmaps.get(heightmapType);
    }

    @Redirect(
            method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Heightmap;trackUpdate(IIILnet/minecraft/block/BlockState;)Z")
    )
    private boolean skipHeightmapUpdate(Heightmap instance, int x, int y, int z, BlockState state) {
        if (instance == null) {
            return false;
        }
        return instance.trackUpdate(x, y, z, state);
    }

    @Inject(
            method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/Heightmap;trackUpdate(IIILnet/minecraft/block/BlockState;)Z",
                    shift = At.Shift.BEFORE,
                    ordinal = 0
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void updateHeightmapsCombined(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir, int y, ChunkSection chunkSection, boolean bl, int x, int yMod16, int z, BlockState blockState, Block block) {
        Heightmap heightmap0 = this.heightmaps.get(Heightmap.Type.MOTION_BLOCKING);
        Heightmap heightmap1 = this.heightmaps.get(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES);
        Heightmap heightmap2 = this.heightmaps.get(Heightmap.Type.OCEAN_FLOOR);
        Heightmap heightmap3 = this.heightmaps.get(Heightmap.Type.WORLD_SURFACE);
        CombinedHeightmapUpdate.updateHeightmaps(heightmap0, heightmap1, heightmap2, heightmap3, (WorldChunk) (Chunk) this, x, y, z, state);
    }
}
