package me.jellysquid.mods.lithium.mixin.cached_hashcode;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.NeighborGroup.class)
public class BlockNeighborGroupMixin {
    @Shadow
    @Final
    private BlockState self;

    @Shadow
    @Final
    private BlockState other;

    @Shadow
    @Final
    private Direction facing;

    private int hash;

    /**
     * @reason Initialize the object's hashcode and cache it
     */
    @Inject(method = "<init>(Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/Direction;)V", at = @At("RETURN"))
    private void generateHash(BlockState blockState_1, BlockState blockState_2, Direction direction_1, CallbackInfo ci) {
        int hash = this.self.hashCode();
        hash = 31 * hash + this.other.hashCode();
        hash = 31 * hash + this.facing.hashCode();

        this.hash = hash;
    }

    /**
     * @reason Uses the cached hashcode
     * @author JellySquid
     */
    @Overwrite(remap = false)
    public int hashCode() {
        return this.hash;
    }
}
