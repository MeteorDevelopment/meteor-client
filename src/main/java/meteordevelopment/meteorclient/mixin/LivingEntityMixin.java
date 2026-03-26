/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.CanWalkOnFluidEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.HighJump;
import meteordevelopment.meteorclient.systems.modules.movement.Sprint;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFlightModes;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.modes.Bounce;
import meteordevelopment.meteorclient.systems.modules.player.NoStatusEffects;
import meteordevelopment.meteorclient.systems.modules.player.OffhandCrash;
import meteordevelopment.meteorclient.systems.modules.render.HandView;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @ModifyReturnValue(method = "canStandOnFluid", at = @At("RETURN"))
    private boolean onCanWalkOnFluid(boolean original, FluidState fluidState) {
        if ((Object) this != mc.player) return original;
        CanWalkOnFluidEvent event = MeteorClient.EVENT_BUS.post(CanWalkOnFluidEvent.get(fluidState));

        return event.walkOnFluid;
    }

    @Inject(method = "spawnItemParticles", at = @At("HEAD"), cancellable = true)
    private void spawnItemParticles(ItemStack stack, int count, CallbackInfo info) {
        NoRender noRender = Modules.get().get(NoRender.class);
        if (noRender.noEatParticles() && stack.getComponents().has(DataComponents.FOOD)) info.cancel();
    }

    @Inject(method = "onEquipItem", at = @At("HEAD"), cancellable = true)
    private void onEquipStack(EquipmentSlot slot, ItemStack oldStack, ItemStack newStack, CallbackInfo info) {
        if ((Object) this != mc.player) return;

        if (Modules.get().get(OffhandCrash.class).isAntiCrash()) {
            info.cancel();
        }
    }

    @ModifyArg(method = "swing(Lnet/minecraft/world/InteractionHand;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;swing(Lnet/minecraft/world/InteractionHand;Z)V"))
    private InteractionHand setHand(InteractionHand hand) {
        if ((Object) this != mc.player) return hand;

        HandView handView = Modules.get().get(HandView.class);
        if (handView.isActive()) {
            if (handView.swingMode.get() == HandView.SwingMode.None) return hand;
            return handView.swingMode.get() == HandView.SwingMode.Offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        }
        return hand;
    }

    @ModifyExpressionValue(method = "getCurrentSwingDuration", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/SwingAnimation;duration()I"))
    private int getHandSwingDuration(int original) {
        if ((Object) this != mc.player) return original;

        return Modules.get().get(HandView.class).isActive() && mc.options.getCameraType().isFirstPerson() ? Modules.get().get(HandView.class).swingSpeed.get() : original;
    }

    @ModifyReturnValue(method = "isFallFlying", at = @At("RETURN"))
    private boolean isGlidingHook(boolean original) {
        if ((Object) this != mc.player) return original;

        if (Modules.get().get(ElytraFly.class).canPacketEfly()) {
            return true;
        }

        return original;
    }

    @Unique
    private boolean previousElytra = false;

    @Inject(method = "isFallFlying", at = @At("TAIL"), cancellable = true)
    public void recastOnLand(CallbackInfoReturnable<Boolean> cir) {
        boolean elytra = cir.getReturnValue();
        ElytraFly elytraFly = Modules.get().get(ElytraFly.class);
        if (previousElytra && !elytra && elytraFly.isActive() && elytraFly.flightMode.get() == ElytraFlightModes.Bounce) {
            cir.setReturnValue(Bounce.recastElytra(mc.player));
        }
        previousElytra = elytra;
    }

    @ModifyReturnValue(method = "hasEffect", at = @At("RETURN"))
    private boolean hasStatusEffect(boolean original, Holder<MobEffect> effect) {
        if (effect == null || effect.value() == null) return original;
        if (Modules.get().get(NoStatusEffects.class).shouldBlock(effect.value())) return false;

        return original;
    }

    @ModifyExpressionValue(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
    private float modifyGetYaw(float original) {
        if ((Object) this != mc.player) return original;
        if (!Modules.get().get(Sprint.class).rageSprint()) return original;

        float forward = Math.signum(mc.player.zza);
        float strafe = 90 * Math.signum(mc.player.xxa);
        if (forward != 0) strafe *= (forward * 0.5f);

        original -= strafe;
        if (forward < 0) original -= 180;

        return original;
    }

    @ModifyConstant(method = "jumpFromGround", constant = @Constant(floatValue = 1.0E-5F))
    private float modifyJumpConstant(float original) {
        if ((Object) this != mc.player) return original;
        if (!Modules.get().isActive(HighJump.class)) return original;
        return -1;
    }

    @ModifyExpressionValue(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSprinting()Z"))
    private boolean modifyIsSprinting(boolean original) {
        if ((Object) this != mc.player) return original;
        if (!Modules.get().get(Sprint.class).rageSprint()) return original;

        // only add the extra velocity if you're actually moving, otherwise you'll jump in place and move forward
        return original && (Math.abs(mc.player.zza) > 1.0E-5F || Math.abs(mc.player.xxa) > 1.0E-5F);
    }
}
