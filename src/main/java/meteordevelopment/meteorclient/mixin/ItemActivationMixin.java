/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mc.MixinTargets;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels the totem of undying activation animation.
 *
 * <p>26.2 triggers it from {@code GameRenderer#displayItemActivation}, 26.3 and 26.4 from
 * {@code LocalPlayer#displayItemActivation}.
 */
@Mixin(targets = MixinTargets.ITEM_ACTIVATION_TARGET)
public abstract class ItemActivationMixin {
    @Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true)
    private void onDisplayItemActivation(ItemStack itemStack, CallbackInfo ci) {
        if (itemStack.getItem() == Items.TOTEM_OF_UNDYING && Modules.get().get(NoRender.class).noTotemAnimation()) {
            ci.cancel();
        }
    }
}
