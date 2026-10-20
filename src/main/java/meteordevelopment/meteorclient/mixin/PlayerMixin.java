/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.DropItemsEvent;
import meteordevelopment.meteorclient.events.entity.player.ClipAtLedgeEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Flight;
import meteordevelopment.meteorclient.systems.modules.movement.NoSlow;
import meteordevelopment.meteorclient.systems.modules.movement.Sprint;
import meteordevelopment.meteorclient.systems.modules.player.Reach;
import meteordevelopment.meteorclient.systems.modules.player.SpeedMine;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {
    @Shadow
    public abstract Abilities getAbilities();

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "isStayingOnGroundSurface", at = @At("HEAD"), cancellable = true)
    protected void clipAtLedge(CallbackInfoReturnable<Boolean> cir) {
        if (!level().isClientSide()) return;

        ClipAtLedgeEvent event = MeteorClient.EVENT_BUS.post(ClipAtLedgeEvent.get());
        if (event.isSet()) cir.setReturnValue(event.isClip());
    }

    @Inject(method = "isSpectator", at = @At("HEAD"), cancellable = true)
    private void onIsSpectator(CallbackInfoReturnable<Boolean> cir) {
        if (mc.getConnection() == null) cir.setReturnValue(false);
    }

    @Inject(method = "isCreative", at = @At("HEAD"), cancellable = true)
    private void onIsCreative(CallbackInfoReturnable<Boolean> cir) {
        if (mc.getConnection() == null) cir.setReturnValue(false);
    }

    @ModifyReturnValue(method = "getDestroySpeed", at = @At(value = "RETURN"))
    public float onGetBlockBreakingSpeed(float breakSpeed, BlockState state) {
        SpeedMine speedMine = Modules.get().get(SpeedMine.class);
        if (state.getBlock() != null && speedMine.isActive() && speedMine.mode.get() == SpeedMine.Mode.Normal && speedMine.filter(state.getBlock()))
            return breakSpeed * speedMine.modifier.get().floatValue();

        return breakSpeed;
    }

    @ModifyReturnValue(method = "getSpeed", at = @At("RETURN"))
    private float onGetSpeed(float original) {
        if (!level().isClientSide()) return original;
        if (!Modules.get().get(NoSlow.class).slowness()) return original;

        float walkSpeed = getAbilities().getWalkingSpeed();

        if (original < walkSpeed) {
            if (isSprinting()) return (float) (walkSpeed * 1.30000001192092896);
            else return walkSpeed;
        }

        return original;
    }

    @Inject(method = "getFlyingSpeed", at = @At("HEAD"), cancellable = true)
    private void onGetFlyingSpeed(CallbackInfoReturnable<Float> cir) {
        if (!level().isClientSide()) return;

        float speed = Modules.get().get(Flight.class).getFlyingSpeed();
        if (speed != -1) cir.setReturnValue(speed);
    }

    @WrapWithCondition(method = "causeExtraKnockback", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private boolean keepSprint$setDeltaMovement(Player instance, Vec3 vec3d) {
        return Modules.get().get(Sprint.class).stopSprinting();
    }

    @WrapWithCondition(method = "causeExtraKnockback", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V"))
    private boolean keepSprint$setSprinting(Player instance, boolean b) {
        return Modules.get().get(Sprint.class).stopSprinting();
    }

    @ModifyReturnValue(method = "blockInteractionRange", at = @At("RETURN"))
    private double modifyBlockInteractionRange(double original) {
        return Math.max(0, original + Modules.get().get(Reach.class).blockReach());
    }

    @ModifyReturnValue(method = "entityInteractionRange", at = @At("RETURN"))
    private double modifyEntityInteractionRange(double original) {
        return Math.max(0, original + Modules.get().get(Reach.class).entityReach());
    }

    @Inject(method = "isWithinAttackRange", at = @At("HEAD"), cancellable = true)
    private void canReachAttackIn(ItemStack weaponItem, AABB box, double additionalRange, CallbackInfoReturnable<Boolean> cir) {
        if (Modules.get().get(Reach.class).isActive()) cir.setReturnValue(true);
    }
    
    @Inject(method = "isWithinBlockInteractionRange", at = @At("HEAD"), cancellable = true)
    private void canReachBlock(BlockPos pos, double additionalRange, CallbackInfoReturnable<Boolean> cir) {
        if (Modules.get().get(Reach.class).isActive()) cir.setReturnValue(true);
    } 
}
