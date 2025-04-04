/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import motordevelopment.motorclient.MotorClient;
import motordevelopment.motorclient.events.entity.player.FinishUsingItemEvent;
import motordevelopment.motorclient.events.entity.player.StoppedUsingItemEvent;
import motordevelopment.motorclient.events.game.ItemStackTooltipEvent;
import motordevelopment.motorclient.systems.modules.Modules;
import motordevelopment.motorclient.systems.modules.render.BetterTooltips;
import motordevelopment.motorclient.utils.Utils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static motordevelopment.motorclient.MotorClient.mc;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @ModifyReturnValue(method = "getTooltip", at = @At("RETURN"))
    private List<Text> onGetTooltip(List<Text> original) {
        if (Utils.canUpdate()) {
            ItemStackTooltipEvent event = MotorClient.EVENT_BUS.post(new ItemStackTooltipEvent((ItemStack) (Object) this, original));
            return event.list();
        }

        return original;
    }

    @ModifyExpressionValue(method = "getTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/BlockPredicatesChecker;showInTooltip()Z", ordinal = 0))
    private boolean modifyCanBreakText(boolean original) {
        BetterTooltips bt = Modules.get().get(BetterTooltips.class);
        return (bt.isActive() && bt.canBreak.get()) || original;
    }

    @ModifyExpressionValue(method = "getTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/BlockPredicatesChecker;showInTooltip()Z", ordinal = 1))
    private boolean modifyCanPlaceText(boolean original) {
        BetterTooltips bt = Modules.get().get(BetterTooltips.class);
        return (bt.isActive() && bt.canPlaceOn.get()) || original;
    }

    @ModifyExpressionValue(method = "getTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;contains(Lnet/minecraft/component/ComponentType;)Z", ordinal = 0))
    private boolean modifyContainsTooltip(boolean original) {
        BetterTooltips bt = Modules.get().get(BetterTooltips.class);
        return !(bt.isActive() && bt.tooltip.get()) && original;
    }

    @ModifyExpressionValue(method = "getTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;contains(Lnet/minecraft/component/ComponentType;)Z", ordinal = 2))
    private boolean modifyContainsAdditional(boolean original) {
        BetterTooltips bt = Modules.get().get(BetterTooltips.class);
        return !(bt.isActive() && bt.additional.get()) && original;
    }

    @Inject(method = "finishUsing", at = @At("HEAD"))
    private void onFinishUsing(World world, LivingEntity user, CallbackInfoReturnable<ItemStack> info) {
        if (user == mc.player) {
            MotorClient.EVENT_BUS.post(FinishUsingItemEvent.get((ItemStack) (Object) this));
        }
    }

    @Inject(method = "onStoppedUsing", at = @At("HEAD"))
    private void onStoppedUsing(World world, LivingEntity user, int remainingUseTicks, CallbackInfo info) {
        if (user == mc.player) {
            MotorClient.EVENT_BUS.post(StoppedUsingItemEvent.get((ItemStack) (Object) this));
        }
    }

    @ModifyExpressionValue(method = "appendAttributeModifiersTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/component/type/AttributeModifiersComponent;showInTooltip()Z"))
    private boolean modifyShowInTooltip(boolean original) {
        BetterTooltips bt = Modules.get().get(BetterTooltips.class);
        return (bt.isActive() && bt.attributeModifiers.get()) || original;
    }
}
