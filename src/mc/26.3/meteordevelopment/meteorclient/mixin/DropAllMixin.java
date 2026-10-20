/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.DropItemsEvent;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Posts the {@link DropItemsEvent} when the whole hotbar is dropped (Q) and cancels the drop
 * when the event is cancelled.
 *
 * <p>26.2 drops the hotbar from {@code LocalPlayer#drop}, 26.3 and 26.4 from
 * {@code MultiPlayerGameMode#dropItem}.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class DropAllMixin {
    @Inject(method = "dropItem", at = @At("HEAD"), cancellable = true)
    private void onDrop(LocalPlayer player, boolean all, CallbackInfo cir) {
        if (MeteorClient.EVENT_BUS.post(DropItemsEvent.get(player.getMainHandItem())).isCancelled())
            cir.cancel();
    }
}
