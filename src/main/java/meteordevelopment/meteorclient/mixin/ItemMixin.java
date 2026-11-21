/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.TooltipDataEvent;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(Item.class)
public abstract class ItemMixin {
    @Inject(method = "getTooltipImage", at = @At("HEAD"), cancellable = true)
    private void onTooltipData(ItemStack itemStack, CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        TooltipDataEvent event = MeteorClient.EVENT_BUS.post(TooltipDataEvent.get(itemStack));
        if (event.tooltipData != null) {
            cir.setReturnValue(Optional.of(event.tooltipData));
        }
    }

    @ModifyExpressionValue(method = "getPlayerPOVHitResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;calculateViewVector(FF)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 fixRotation(Vec3 original, Level level, Player player, ClipContext.Fluid fluid) {
        if (player == mc.player && Rotations.rotating) return Rotations.getServerDirection();
        return original;
    }
}
