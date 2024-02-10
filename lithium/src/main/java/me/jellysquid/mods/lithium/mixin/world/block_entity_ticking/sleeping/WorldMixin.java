package me.jellysquid.mods.lithium.mixin.world.block_entity_ticking.sleeping;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(World.class)
public class WorldMixin {

    @Redirect(
            method = "tickBlockEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;shouldTickBlockPos(Lnet/minecraft/util/math/BlockPos;)Z" )
    )
    private boolean shouldTickBlockPosFilterNull(World instance, BlockPos pos) {
        if (pos == null) {
            return false;
        }
        return instance.shouldTickBlockPos(pos);
    }
}
