/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import motordevelopment.motorclient.MotorClient;
import motordevelopment.motorclient.events.entity.player.CanWalkOnFluidEvent;
import motordevelopment.motorclient.systems.modules.Modules;
import motordevelopment.motorclient.systems.modules.movement.Sprint;
import motordevelopment.motorclient.systems.modules.movement.elytrafly.ElytraFlightModes;
import motordevelopment.motorclient.systems.modules.movement.elytrafly.ElytraFly;
import motordevelopment.motorclient.systems.modules.movement.elytrafly.modes.Bounce;
import motordevelopment.motorclient.systems.modules.player.OffhandCrash;
import motordevelopment.motorclient.systems.modules.player.PotionSpoof;
import motordevelopment.motorclient.systems.modules.render.HandView;
import motordevelopment.motorclient.systems.modules.render.NoRender;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static motordevelopment.motorclient.MotorClient.mc;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @ModifyReturnValue(method = "canWalkOnFluid", at = @At("RETURN"))
    private boolean onCanWalkOnFluid(boolean original, FluidState fluidState) {
        if ((Object) this != mc.player) return original;
        CanWalkOnFluidEvent event = MotorClient.EVENT_BUS.post(CanWalkOnFluidEvent.get(fluidState));

        return event.walkOnFluid;
    }

    @Inject(method = "spawnItemParticles", at = @At("HEAD"), cancellable = true)
    private void spawnItemParticles(ItemStack stack, int count, CallbackInfo info) {
        NoRender noRender = Modules.get().get(NoRender.class);
        if (noRender.noEatParticles() && stack.getComponents().contains(DataComponentTypes.FOOD)) info.cancel();
    }

    @Inject(method = "onEquipStack", at = @At("HEAD"), cancellable = true)
    private void onEquipStack(EquipmentSlot slot, ItemStack oldStack, ItemStack newStack, CallbackInfo info) {
        if ((Object) this != mc.player) return;

        if (Modules.get().get(OffhandCrash.class).isAntiCrash()) {
            info.cancel();
        }
    }

    @ModifyArg(method = "swingHand(Lnet/minecraft/util/Hand;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;swingHand(Lnet/minecraft/util/Hand;Z)V"))
    private Hand setHand(Hand hand) {
        if ((Object) this != mc.player) return hand;

        HandView handView = Modules.get().get(HandView.class);
        if (handView.isActive()) {
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

    @ModifyReturnValue(method = "isGliding", at = @At("RETURN"))
    private boolean isGlidingHook(boolean original) {
        if ((Object) this != mc.player) return original;

        if (Modules.get().get(ElytraFly.class).canPacketEfly()) {
            return true;
        }

        return original;
    }

    @Unique
    private boolean previousElytra = false;

    @Inject(method = "isGliding", at = @At("TAIL"), cancellable = true)
    public void recastOnLand(CallbackInfoReturnable<Boolean> cir) {
        boolean elytra = cir.getReturnValue();
        ElytraFly elytraFly = Modules.get().get(ElytraFly.class);
        if (previousElytra && !elytra && elytraFly.isActive() && elytraFly.flightMode.get() == ElytraFlightModes.Bounce) {
            cir.setReturnValue(Bounce.recastElytra(mc.player));
        }
        previousElytra = elytra;
    }

    @ModifyReturnValue(method = "hasStatusEffect", at = @At("RETURN"))
    private boolean hasStatusEffect(boolean original, RegistryEntry<StatusEffect> effect) {
        if (Modules.get().get(PotionSpoof.class).shouldBlock(effect.value())) return false;

        return original;
    }

    @ModifyExpressionValue(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"))
    private float modifyGetYaw(float original) {
        if ((Object) this != mc.player) return original;
        if (!Modules.get().get(Sprint.class).rageSprint()) return original;

        float forward = Math.signum(mc.player.input.movementForward);
        float strafe = 90 * Math.signum(mc.player.input.movementSideways);
        if (forward != 0) strafe *= (forward * 0.5f);

        original -= strafe;
        if (forward < 0) original -= 180;

        return original;
    }

    @ModifyExpressionValue(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isSprinting()Z"))
    private boolean modifyIsSprinting(boolean original) {
        if ((Object) this != mc.player) return original;
        if (!Modules.get().get(Sprint.class).rageSprint()) return original;

        // only add the extra velocity if you're actually moving, otherwise you'll jump in place and move forward
        return original && (Math.abs(mc.player.input.movementForward) > 1.0E-5F || Math.abs(mc.player.input.movementSideways) > 1.0E-5F);
    }
}
