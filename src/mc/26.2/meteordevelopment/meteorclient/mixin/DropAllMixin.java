/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.DropItemsEvent;
import net.minecraft.client.player.LocalPlayer;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Posts the {@link DropItemsEvent} when the whole hotbar is dropped (Q) and cancels the drop
 * when the event is cancelled.
 *
 * <p>26.2 drops the hotbar from {@code LocalPlayer#drop}, 26.3 and 26.4 from
 * {@code MultiPlayerGameMode#dropItem}.
 */
@Mixin(LocalPlayer.class)
public abstract class DropAllMixin {
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void onDrop(boolean all, CallbackInfoReturnable<Boolean> cir) {
        if (MeteorClient.EVENT_BUS.post(DropItemsEvent.get(((LocalPlayer) (Object) this).getMainHandItem())).isCancelled())
            cir.setReturnValue(false);
    }
}
