/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.InventoryTweaks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net/minecraft/screen/PlayerScreenHandler$1")
public class PlayerArmorSlotMixin {

    @Inject(method = "getMaxItemCount", at = @At("HEAD"), cancellable = true)
    private void onGetMaxItemCount(CallbackInfoReturnable<Integer> cir) {
        if (Modules.get().get(InventoryTweaks.class).armorStorage()) cir.setReturnValue(64);
    }

    @Inject(method = "canInsert", at = @At("HEAD"), cancellable = true)
    private void onCanInsert(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (Modules.get().get(InventoryTweaks.class).armorStorage()) cir.setReturnValue(true);
    }

    @Inject(method = "canTakeItems", at = @At("HEAD"), cancellable = true)
    private void onCanTakeItems(PlayerEntity playerEntity, CallbackInfoReturnable<Boolean> cir) {
        if (Modules.get().get(InventoryTweaks.class).armorStorage()) cir.setReturnValue(true);
    }
}
