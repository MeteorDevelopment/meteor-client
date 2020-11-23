/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.mixininterface.IPlayerEntity;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.movement.SafeWalk;
import minegame159.meteorclient.modules.movement.Scaffold;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin implements IPlayerEntity {
    @Shadow @Final @Mutable public PlayerInventory inventory;

    @Inject(method = "clipAtLedge", at = @At("HEAD"), cancellable = true)
    protected void clipAtLedge(CallbackInfoReturnable<Boolean> info) {
        Scaffold scaffold = ModuleManager.INSTANCE.get(Scaffold.class);

        if (scaffold.isActive() && (MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().player.input.sneaking)) {
            info.setReturnValue(false);
            return;
        }

        if (ModuleManager.INSTANCE.isActive(SafeWalk.class) || (scaffold.isActive() && scaffold.hasSafeWalk())) {
            info.setReturnValue(true);
        }
    }

    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"))
    private void onDropItem(ItemStack stack, boolean bl, boolean bl2, CallbackInfoReturnable<ItemEntity> info) {
        MeteorClient.EVENT_BUS.post(EventStore.dropItemEvent(stack));
    }

    @Override
    public void setInventory(PlayerInventory inventory) {
        this.inventory = inventory;
    }
}
