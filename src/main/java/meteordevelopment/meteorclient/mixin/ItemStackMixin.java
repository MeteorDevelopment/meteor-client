/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.FinishUsingItemEvent;
import meteordevelopment.meteorclient.events.entity.player.StoppedUsingItemEvent;
import meteordevelopment.meteorclient.events.game.ItemStackTooltipEvent;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @ModifyReturnValue(method = "getTooltipLines", at = @At("RETURN"))
    private List<Component> onGetTooltip(List<Component> original) {
        if (Utils.canUpdate()) {
            ItemStackTooltipEvent event = MeteorClient.EVENT_BUS.post(new ItemStackTooltipEvent((ItemStack) (Object) this, original));
            return event.list();
        }

        return original;
    }

    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void onFinishUsing(Level world, LivingEntity user, CallbackInfoReturnable<ItemStack> info) {
        if (user == mc.player) {
            MeteorClient.EVENT_BUS.post(FinishUsingItemEvent.get((ItemStack) (Object) this));
        }
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"))
    private void onStoppedUsing(Level world, LivingEntity user, int remainingUseTicks, CallbackInfo info) {
        if (user == mc.player) {
            MeteorClient.EVENT_BUS.post(StoppedUsingItemEvent.get((ItemStack) (Object) this));
        }
    }
}
