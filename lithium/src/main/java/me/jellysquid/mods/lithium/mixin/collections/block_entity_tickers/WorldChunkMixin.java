package me.jellysquid.mods.lithium.mixin.collections.block_entity_tickers;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.BlendingData;
import net.minecraft.world.tick.ChunkTickScheduler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(WorldChunk.class)
public class WorldChunkMixin {
    @Mutable
    @Shadow
    @Final
    private Map<BlockPos, ?> blockEntityTickers;

    @Inject(
            method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/chunk/UpgradeData;Lnet/minecraft/world/tick/ChunkTickScheduler;Lnet/minecraft/world/tick/ChunkTickScheduler;J[Lnet/minecraft/world/chunk/ChunkSection;Lnet/minecraft/world/chunk/WorldChunk$EntityLoader;Lnet/minecraft/world/gen/chunk/BlendingData;)V",
            at = @At(value = "INVOKE_ASSIGN", target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;", shift = At.Shift.AFTER, remap = false)
    )
    @Coerce
    private void createFastUtilMap(World world, ChunkPos pos, UpgradeData upgradeData, ChunkTickScheduler<?> blockTickScheduler, ChunkTickScheduler<?> fluidTickScheduler, long inhabitedTime, ChunkSection[] sectionArrayInitializer, WorldChunk.EntityLoader entityLoader, BlendingData blendingData, CallbackInfo ci) {
        this.blockEntityTickers = new Object2ObjectOpenHashMap<>();
    }
}
