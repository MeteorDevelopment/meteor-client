package me.jellysquid.mods.lithium.mixin.entity.skip_equipment_change_check;

import me.jellysquid.mods.lithium.common.entity.EquipmentEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorStandEntity.class)
public class ArmorStandEntityMixin implements EquipmentEntity.EquipmentTrackingEntity, EquipmentEntity {
    @Inject(
            method = "readCustomDataFromNbt(Lnet/minecraft/nbt/NbtCompound;)V",
            at = @At("RETURN")
    )
    private void trackEquipChange(NbtCompound nbt, CallbackInfo ci) {
        this.lithiumOnEquipmentChanged();
    }

    @Inject(
            method = "equipStack(Lnet/minecraft/entity/EquipmentSlot;Lnet/minecraft/item/ItemStack;)V",
            at = @At("RETURN")
    )
    private void trackEquipChange(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        this.lithiumOnEquipmentChanged();
    }

    @Inject(
            method = "onBreak(Lnet/minecraft/entity/damage/DamageSource;)V",
            at = @At("RETURN")
    )
    private void trackEquipChange(DamageSource damageSource, CallbackInfo ci) {
        this.lithiumOnEquipmentChanged();
    }
}
