/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.DropItemsEvent;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Posts the {@link DropItemsEvent} for a single dropped stack and cancels the drop when the
 * event is cancelled.
 *
 * <p>26.2 declares {@code drop} on {@code Player}, 26.3 and 26.4 on {@code LivingEntity}
 * (with a {@link Prediction} argument).
 */
@Mixin(Player.class)
public abstract class ItemDropMixin {
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void onDropItem(ItemStack itemStack, boolean thrownFromHand, CallbackInfoReturnable<ItemEntity> cir) {
        if (((Entity) (Object) this).level().isClientSide() && !itemStack.isEmpty()) {
            if (MeteorClient.EVENT_BUS.post(DropItemsEvent.get(itemStack)).isCancelled()) cir.setReturnValue(null);
        }
    }
}
