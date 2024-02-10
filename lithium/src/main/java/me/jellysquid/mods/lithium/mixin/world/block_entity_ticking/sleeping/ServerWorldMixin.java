package me.jellysquid.mods.lithium.mixin.world.block_entity_ticking.sleeping;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {

    @Redirect(
            method = "dumpBlockEntities(Ljava/io/Writer;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/BlockEntityTickInvoker;getPos()Lnet/minecraft/util/math/BlockPos;")
    )
    private BlockPos getPosOrOrigin(BlockEntityTickInvoker instance) {
        BlockPos pos = instance.getPos();
        if (pos == null) {
            return BlockPos.ORIGIN;
        }
        return pos;
    }
}
