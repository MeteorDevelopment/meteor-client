/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.DamageEvent;
import meteordevelopment.meteorclient.events.entity.player.CanWalkOnFluidEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly;
import meteordevelopment.meteorclient.systems.modules.player.OffhandCrash;
import meteordevelopment.meteorclient.systems.modules.player.PotionSpoof;
import meteordevelopment.meteorclient.systems.modules.render.HandView;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Shadow
    @Final
    private Map<StatusEffect, StatusEffectInstance> activeStatusEffects;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "damage", at = @At("HEAD"))
    private void onDamageHead(DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
        if (Utils.canUpdate() && world.isClient)
            MeteorClient.EVENT_BUS.post(DamageEvent.get((LivingEntity) (Object) this, source));
    }

    @Inject(method = "canWalkOnFluid", at = @At("HEAD"), cancellable = true)
    private void onCanWalkOnFluid(FluidState fluidState, CallbackInfoReturnable<Boolean> info) {
        if ((Object) this != mc.player) return;
        CanWalkOnFluidEvent event = MeteorClient.EVENT_BUS.post(CanWalkOnFluidEvent.get(fluidState));

        info.setReturnValue(event.walkOnFluid);
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;hasNoGravity()Z"))
    private boolean travelHasNoGravityProxy(LivingEntity self) {
        if (activeStatusEffects.containsKey(StatusEffects.LEVITATION) && Modules.get().get(PotionSpoof.class).shouldBlock(StatusEffects.LEVITATION)) {
            return !Modules.get().get(PotionSpoof.class).applyGravity.get();
        }
        return self.hasNoGravity();
    }

    @Inject(method = "spawnItemParticles", at = @At("HEAD"), cancellable = true)
    private void spawnItemParticles(ItemStack stack, int count, CallbackInfo info) {
        NoRender noRender = Modules.get().get(NoRender.class);
        if (noRender.noEatParticles() && stack.isFood()) info.cancel();
    }

    @Inject(method = "onEquipStack", at = @At("HEAD"), cancellable = true)
    private void onEquipStack(EquipmentSlot slot, ItemStack oldStack, ItemStack newStack, CallbackInfo info) {
        if ((Object) this == mc.player && Modules.get().get(OffhandCrash.class).isAntiCrash()) {
            info.cancel();
        }
    }

    @ModifyArg(method = "swingHand(Lnet/minecraft/util/Hand;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;swingHand(Lnet/minecraft/util/Hand;Z)V"))
    private Hand setHand(Hand hand) {
        HandView handView = Modules.get().get(HandView.class);
        if ((Object) this == mc.player && handView.isActive()) {
            if (handView.swingMode.get() == HandView.SwingMode.None) return hand;
            return handView.swingMode.get() == HandView.SwingMode.Offhand ? Hand.OFF_HAND : Hand.MAIN_HAND;
        }
        return hand;
    }

    @ModifyConstant(method = "getHandSwingDuration", constant = @Constant(intValue = 6))
    private int getHandSwingDuration(int constant) {
        if ((Object) this != mc.player) return constant;
        return Modules.get().get(HandView.class).isActive() && mc.options.getPerspective().isFirstPerson() ? Modules.get().get(HandView.class).swingSpeed.get() : constant;
    }

    @Inject(method = "isFallFlying", at = @At("HEAD"), cancellable = true)
    private void isFallFlyingHook(CallbackInfoReturnable<Boolean> info) {
        if ((Object) this == mc.player && Modules.get().get(ElytraFly.class).canPacketEfly()) {
            info.setReturnValue(true);
        }
    }

    @Inject(method = "hasStatusEffect", at = @At("HEAD"), cancellable = true)
    private void hasStatusEffect(StatusEffect effect, CallbackInfoReturnable<Boolean> info) {
        if (Modules.get().get(PotionSpoof.class).shouldBlock(effect)) info.setReturnValue(false);
    }
}
