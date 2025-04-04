/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import motordevelopment.motorclient.systems.modules.Modules;
import motordevelopment.motorclient.systems.modules.render.BetterTooltips;
import net.minecraft.item.BundleItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BundleItem.class)
public abstract class BundleItemMixin {
    @ModifyExpressionValue(method = "getTooltipData", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;contains(Lnet/minecraft/component/ComponentType;)Z", ordinal = 0))
    private boolean modifyContains1(boolean original) {
        BetterTooltips bt = Modules.get().get(BetterTooltips.class);
        return !(bt.isActive() && bt.tooltip.get()) && original;
    }

    @ModifyExpressionValue(method = "getTooltipData", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;contains(Lnet/minecraft/component/ComponentType;)Z", ordinal = 1))
    private boolean modifyContains2(boolean original) {
        BetterTooltips bt = Modules.get().get(BetterTooltips.class);
        return !(bt.isActive() && bt.additional.get()) && original;
    }
}
