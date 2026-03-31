/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.NonNullList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemContainerContents.class)
public abstract class ItemContainerContentsMixin {
    @Shadow
    @Final
    private NonNullList<ItemStack> items;

    @Inject(method = "appendTooltip", at = @At("HEAD"), cancellable = true)
    private void onAppendTooltip(Item.TooltipContext context, Consumer<Component> textConsumer, TooltipFlag type, DataComponentGetter components, CallbackInfo ci) {
        if (Modules.get() == null) return;

        BetterTooltips tooltips = Modules.get().get(BetterTooltips.class);
        if (tooltips.isActive()) {
            if (tooltips.previewShulkers()) ci.cancel();
            else if (tooltips.shulkerCompactTooltip()) {
                ci.cancel();
                tooltips.applyCompactShulkerTooltip(items, textConsumer);
            }
        }
    }
}
