package me.jellysquid.mods.lithium.mixin.util.inventory_change_listening;

import me.jellysquid.mods.lithium.common.block.entity.inventory_change_tracking.InventoryChangeEmitter;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityMixin extends LootableContainerBlockEntity implements InventoryChangeEmitter {
    protected ChestBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @SuppressWarnings("deprecation")
    @Intrinsic
    @Override
    public void setCachedState(BlockState state) {
        super.setCachedState(state);
    }

    @SuppressWarnings({"MixinAnnotationTarget", "UnresolvedMixinReference"})
    @Inject(method = "setCachedState(Lnet/minecraft/block/BlockState;)V", at = @At("RETURN"))
    private void emitRemovedOnSetCachedState(CallbackInfo ci) {
        //Handle switching double / single chest state
        this.emitRemoved();
    }
}
