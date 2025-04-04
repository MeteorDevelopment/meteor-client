/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import motordevelopment.motorclient.systems.modules.Modules;
import motordevelopment.motorclient.systems.modules.movement.TridentBoost;
import motordevelopment.motorclient.utils.Utils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static motordevelopment.motorclient.MotorClient.mc;

@Mixin(TridentItem.class)
public abstract class TridentItemMixin {
    @Inject(method = "onStoppedUsing", at = @At("HEAD"))
    private void onStoppedUsingHead(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> info) {
        if (user == mc.player) Utils.isReleasingTrident = true;
    }

    @Inject(method = "onStoppedUsing", at = @At("TAIL"))
    private void onStoppedUsingTail(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> info) {
        if (user == mc.player) Utils.isReleasingTrident = false;
    }

    @ModifyArgs(method = "onStoppedUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;addVelocity(DDD)V"))
    private void modifyVelocity(Args args) {
        TridentBoost tridentBoost = Modules.get().get(TridentBoost.class);

        args.set(0, (double) args.get(0) * tridentBoost.getMultiplier());
        args.set(1, (double) args.get(1) * tridentBoost.getMultiplier());
        args.set(2, (double) args.get(2) * tridentBoost.getMultiplier());
    }

    @ModifyExpressionValue(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isTouchingWaterOrRain()Z"))
    private boolean isInWaterUse(boolean original) {
        TridentBoost tridentBoost = Modules.get().get(TridentBoost.class);

        return tridentBoost.allowOutOfWater() || original;
    }

    @ModifyExpressionValue(method = "onStoppedUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isTouchingWaterOrRain()Z"))
    private boolean isInWaterPostUse(boolean original) {
        TridentBoost tridentBoost = Modules.get().get(TridentBoost.class);

        return tridentBoost.allowOutOfWater() || original;
    }
}
