/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.InventoryTweaks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net/minecraft/screen/PlayerScreenHandler$1")
public class PlayerArmorSlotMixin {
    @ModifyReturnValue(method = "getMaxItemCount", at = @At("RETURN"))
    private int onGetMaxItemCount(int original) {
        if (Modules.get().get(InventoryTweaks.class).armorStorage()) return 64;
        return original;
    }

    @ModifyReturnValue(method = "canInsert", at = @At("RETURN"))
    private boolean onCanInsert(boolean original, ItemStack stack) {
        if (Modules.get().get(InventoryTweaks.class).armorStorage()) return true;
        return original;
    }

    @ModifyReturnValue(method = "canTakeItems", at = @At("RETURN"))
    private boolean onCanTakeItems(boolean original, PlayerEntity playerEntity) {
        if (Modules.get().get(InventoryTweaks.class).armorStorage()) return true;
        return original;
    }
}
