package me.jellysquid.mods.lithium.mixin.experimental.util.item_entity_by_type;

import me.jellysquid.mods.lithium.common.hopper.NotifyingItemStack;
import me.jellysquid.mods.lithium.mixin.util.accessors.ItemStackAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {
    public ItemEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow
    public abstract ItemStack getStack();

    @Redirect(
            method = "setStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/data/DataTracker;set(Lnet/minecraft/entity/data/TrackedData;Ljava/lang/Object;)V")
    )
    private <T> void handleItemTypeChange(DataTracker dataTracker, TrackedData<T> key, T newStack) {
        ItemStack oldStack = this.getStack();
        dataTracker.set(key, newStack);

        Item newItem = ((ItemStackAccessor) newStack).lithium$getItem();
        if (newItem != ((ItemStackAccessor) (Object) oldStack).lithium$getItem()) {
            ((NotifyingItemStack) (Object) oldStack).lithium$notifyAfterItemEntityStackSwap((ItemEntity) (Object) this, oldStack);
        }
    }
}
