/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.HandView;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(FirstPersonHandsAndItems.class)
public abstract class FirstPersonHandsAndItemsMixin {
    @Shadow
    protected abstract boolean shouldInstantlyReplaceVisibleItem(ItemStack currentlyVisibleItem, ItemStack expectedItem, LocalPlayer player);

    @Shadow
    private ItemStack mainHandItem;

    @Shadow
    private float mainHandHeight;

    @Shadow
    private ItemStack offHandItem;

    @Shadow
    private float offHandHeight;

    @ModifyReturnValue(method = "shouldInstantlyReplaceVisibleItem", at = @At("RETURN"))
    private boolean modifySkipSwapAnimation(boolean original) {
        return original || Modules.get().get(HandView.class).skipSwapping();
    }

    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F", ordinal = 2), index = 0)
    private float modifyEquipProgressMainhand(float value, @Local(argsOnly = true) LocalPlayer player) {
        HandView handView = Modules.get().get(HandView.class);
        if (handView.swordSlash() && mc.player.getMainHandItem().is(ItemTags.SWORDS)) return value;

        float f = mc.player.getItemSwapScale(1f);
        float modified = handView.oldAnimations() ? 1 : f * f * f;

        return (shouldInstantlyReplaceVisibleItem(mainHandItem, player.getMainHandItem(), player) ? modified : 0) - mainHandHeight;
    }

    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F", ordinal = 3), index = 0)
    private float modifyEquipProgressOffhand(float value, @Local(argsOnly = true) LocalPlayer player) {
        return (shouldInstantlyReplaceVisibleItem(offHandItem, player.getOffhandItem(), player) ? 1 : 0) - offHandHeight;
    }
}
