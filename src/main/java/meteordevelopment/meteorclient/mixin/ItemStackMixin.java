/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import java.util.List;
import java.util.function.Consumer;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.FinishUsingItemEvent;
import meteordevelopment.meteorclient.events.entity.player.StoppedUsingItemEvent;
import meteordevelopment.meteorclient.events.game.ItemStackTooltipEvent;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Damages;
import meteordevelopment.meteorclient.systems.modules.world.Quantities;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.jetbrains.annotations.Nullable; 

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @ModifyReturnValue(method = "getTooltip", at = @At("RETURN"))
    private List<Text> onGetTooltip(List<Text> original) {
        if (Utils.canUpdate()) {
            ItemStackTooltipEvent event = MeteorClient.EVENT_BUS.post(new ItemStackTooltipEvent((ItemStack) (Object) this, original));
            return event.list();
        }

        return original;
    }

    @Inject(method = "finishUsing", at = @At("HEAD"))
    private void onFinishUsing(World world, LivingEntity user, CallbackInfoReturnable<ItemStack> info) {
        if (user == mc.player) {
            MeteorClient.EVENT_BUS.post(FinishUsingItemEvent.get((ItemStack) (Object) this));
        }
    }

    @Inject(method = "onStoppedUsing", at = @At("HEAD"))
    private void onStoppedUsing(World world, LivingEntity user, int remainingUseTicks, CallbackInfo info) {
        if (user == mc.player) {
            MeteorClient.EVENT_BUS.post(StoppedUsingItemEvent.get((ItemStack) (Object) this));
        }
    }

    @Inject(at = @At("HEAD"), method = "damage(ILnet/minecraft/server/world/ServerWorld;Lnet/minecraft/server/network/ServerPlayerEntity;Ljava/util/function/Consumer;)V", cancellable = true)
    private void isDamage(int amount, ServerWorld world, @Nullable ServerPlayerEntity player, Consumer<Item> breakCallback, CallbackInfo ci)
    {
        ItemStack itemStack = (ItemStack)(Object)this;
        Item thisObj = itemStack.getItem();
        if(Modules.get().get(Damages.class).inItemsList(thisObj))
        ci.cancel();
    }

    @Inject(at = @At("HEAD"), method = "increment(I)V", cancellable = true)
    private void incrementControl(int amount, CallbackInfo ci)
    {
        ItemStack itemStack = (ItemStack)(Object)this;
        Item thisObj = itemStack.getItem();
        if (Modules.get().get(Quantities.class).incr(thisObj)) ci.cancel();
    }

    @Inject(at = @At("HEAD"), method = "decrement(I)V", cancellable = true)
    private void decrementControl(int amount, CallbackInfo ci)
    {
        ItemStack itemStack = (ItemStack)(Object)this;
        Item thisObj = itemStack.getItem();
        if (Modules.get().get(Quantities.class).decr(thisObj)) ci.cancel();
    }
}
