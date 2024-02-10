package me.jellysquid.mods.lithium.mixin.world.block_entity_ticking.sleeping;

import net.minecraft.world.chunk.BlockEntityTickInvoker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net/minecraft/world/chunk/WorldChunk$WrappedBlockEntityTickInvoker" )
public interface WrappedBlockEntityTickInvokerAccessor {
    @Invoker
    void callSetWrapped(BlockEntityTickInvoker wrapped);

    @Accessor
    BlockEntityTickInvoker getWrapped();
}
