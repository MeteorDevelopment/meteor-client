package me.jellysquid.mods.lithium.mixin.world.block_entity_ticking.world_border;

import me.jellysquid.mods.lithium.common.world.listeners.WorldBorderListenerOnce;
import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.border.WorldBorderStage;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.chunk.WorldChunk$DirectBlockEntityTickInvoker")
public abstract class DirectBlockEntityTickInvokerMixin implements WorldBorderListenerOnce {

    @Shadow
    @Final
    WorldChunk worldChunk;

    @Shadow
    public abstract BlockPos getPos();

    private byte worldBorderState = 0;

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/WorldChunk;canTickBlockEntity(Lnet/minecraft/util/math/BlockPos;)Z"
            )
    )
    private boolean cachedCanTickBlockEntity(WorldChunk instance, BlockPos pos) {
        if (this.isInsideWorldBorder()) {
            World world = this.worldChunk.getWorld();
            if (world instanceof ServerWorld serverWorld) {
                return this.worldChunk.getLevelType().isAfter(ChunkLevelType.BLOCK_TICKING) && serverWorld.isChunkLoaded(ChunkPos.toLong(pos));
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean isInsideWorldBorder() {
        if (this.worldBorderState == (byte) 0) {
            this.startWorldBorderCaching();
        }

        int worldBorderState = this.worldBorderState;
        if ((worldBorderState & 3) == 3) {
            return (worldBorderState & 4) != 0;
        }
        return this.worldChunk.getWorld().getWorldBorder().contains(this.getPos());
    }

    private void startWorldBorderCaching() {
        this.worldBorderState = (byte) 1;
        WorldBorder worldBorder = this.worldChunk.getWorld().getWorldBorder();
        worldBorder.addListener(this);
        boolean isStationary = worldBorder.getStage() == WorldBorderStage.STATIONARY;
        if (worldBorder.contains(this.getPos())) {
            if (isStationary || worldBorder.getStage() == WorldBorderStage.GROWING) {
                this.worldBorderState |= (byte) 6;
            }
        } else {
            if (isStationary || worldBorder.getStage() == WorldBorderStage.SHRINKING) {
                this.worldBorderState |= (byte) 2;
            }
        }
    }

    @Override
    public void onWorldBorderShapeChange(WorldBorder worldBorder) {
        this.worldBorderState = (byte) 0;
    }
}
