package me.jellysquid.mods.lithium.mixin.util.inventory_change_listening;

import me.jellysquid.mods.lithium.common.block.entity.inventory_change_tracking.InventoryChangeTracker;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public class BlockEntityMixin {

    @Shadow
    @Nullable
    protected World world;

    @Inject(
            method = "markRemoved",
            at = @At("RETURN")
    )
    private void updateStackListTracking(CallbackInfo ci) {
        if (this.world != null && !this.world.isClient() && this instanceof InventoryChangeTracker inventoryChangeTracker) {
            inventoryChangeTracker.emitRemoved();
        }
    }
}
