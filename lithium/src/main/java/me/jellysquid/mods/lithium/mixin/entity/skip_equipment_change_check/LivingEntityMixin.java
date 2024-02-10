package me.jellysquid.mods.lithium.mixin.entity.skip_equipment_change_check;

import me.jellysquid.mods.lithium.common.entity.EquipmentEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.function.Predicate;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements EquipmentEntity {

    private static final Predicate<ItemStack> DYNAMIC_EQUIPMENT = item -> item.isOf(Items.CROSSBOW);

    @Shadow
    public abstract boolean isHolding(Predicate<ItemStack> predicate);

    private boolean equipmentChanged = true;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    public void lithiumOnEquipmentChanged() {
        this.equipmentChanged = true;
    }

    @Inject(
            method = "getEquipmentChanges()Ljava/util/Map;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void skipSentEquipmentComparison(CallbackInfoReturnable<@Nullable Map<EquipmentSlot, ItemStack>> cir) {
        if (!this.equipmentChanged) {
            cir.setReturnValue(null);
        }
    }

    @Inject(
            method = "sendEquipmentChanges()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;checkHandStackSwap(Ljava/util/Map;)V"
            )
    )
    private void resetEquipmentChanged(CallbackInfo ci) {
        //Not implemented for player entities and modded entities, fallback to never skipping inventory comparison
        //Work around dynamic items that are changed while holding them (only crossbow in 1.19.2)
        if (this instanceof EquipmentTrackingEntity && !this.isHolding(DYNAMIC_EQUIPMENT)) {
            this.equipmentChanged = false;
        }
    }

    @Inject(
            method = "eatFood(Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;",
            at = @At("RETURN")
    )
    private void trackEatingEquipmentChange(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        this.lithiumOnEquipmentChanged();
    }
}
