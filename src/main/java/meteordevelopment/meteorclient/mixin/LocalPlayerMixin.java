/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.DropItemsEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerTickMovementEvent;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.*;
import meteordevelopment.meteorclient.systems.modules.player.LiquidInteract;
import meteordevelopment.meteorclient.systems.modules.player.NoMiningTrace;
import meteordevelopment.meteorclient.systems.modules.player.Portals;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {
    @Shadow
    public ClientInput input;

    public LocalPlayerMixin(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void onDrop(boolean dropEntireStack, CallbackInfoReturnable<Boolean> cir) {
        if (MeteorClient.EVENT_BUS.post(DropItemsEvent.get(getMainHandItem())).isCancelled())
            cir.setReturnValue(false);
    }

    @ModifyExpressionValue(method = "handlePortalTransitionEffect", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;", opcode = Opcodes.GETFIELD))
    private Screen modifyPortalTransitionEffect(Screen original) {
        if (Modules.get().isActive(Portals.class)) return null;
        return original;
    }

    @ModifyExpressionValue(method = "modifyInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean redirectUsingItem(boolean isUsingItem) {
        if (Modules.get().get(NoSlow.class).items()) return false;
        return isUsingItem;
    }

    @Inject(method = "isShiftKeyDown", at = @At("HEAD"), cancellable = true)
    private void onIsShiftKeyDown(CallbackInfoReturnable<Boolean> cir) {
        if (Modules.get().get(Scaffold.class).scaffolding()) cir.setReturnValue(false);
        if (Modules.get().get(Flight.class).noSneak()) cir.setReturnValue(false);
    }

    @Inject(method = "isMovingSlowly", at = @At("HEAD"), cancellable = true)
    private void onIsMovingSlowly(CallbackInfoReturnable<Boolean> cir) {
        if (Modules.get().get(NoSlow.class).sneaking()) {
            cir.setReturnValue(isVisuallyCrawling());
        }
    }

    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void onMoveTowardsClosestSpace(double x, double d, CallbackInfo ci) {
        Velocity velocity = Modules.get().get(Velocity.class);
        if (velocity.isActive() && velocity.blocks.get()) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/ClientInput;keyPresses:Lnet/minecraft/world/entity/player/Input;", opcode = Opcodes.GETFIELD))
    private Input isSneaking(Input original) {
        if (Modules.get().get(Sneak.class).doPacket() || Modules.get().get(NoSlow.class).airStrict()) {
            return new Input(
                original.forward(),
                original.backward(),
                original.left(),
                original.right(),
                original.jump(),
                true,
                original.sprint()
            );
        }
        return original;
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void preAiStep(CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(PlayerTickMovementEvent.get());
    }

    @ModifyReturnValue(method = "getJumpRidingScale", at = @At("RETURN"))
    private float modifyJumpRidingScale(float original) {
        if (Modules.get().get(EntityControl.class).maxJump()) return 1f;
        return original;
    }

    @Inject(method = "jumpableVehicle", at = @At("RETURN"), cancellable = true)
    private void changeJumpableVehicle(CallbackInfoReturnable<PlayerRideableJumping> cir) {
        if (Modules.get().get(EntityControl.class).cancelJump()) cir.setReturnValue(null);
    }

    @ModifyReturnValue(method = "pick", at = @At("RETURN"))
    private static HitResult onUpdateTargetedEntity(HitResult original, @Local HitResult hitResult) {
        if (original instanceof EntityHitResult ehr) {
            if (Modules.get().get(NoMiningTrace.class).canWork(ehr.getEntity()) && hitResult.getType() == HitResult.Type.BLOCK) {
                return hitResult;
            } else if (ehr.getEntity() instanceof FakePlayerEntity fakePlayer && fakePlayer.noHit) {
                return hitResult;
            }
        }

        return original;
    }

    @ModifyExpressionValue(method = "pick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;"))
    private static HitResult modifyRaycastResult(HitResult original, Entity entity, double blockInteractionRange, double entityInteractionRange, float tickProgress, @Local(ordinal = 0, argsOnly = true) double maxDistance) {
        if (!Modules.get().isActive(LiquidInteract.class)) return original;
        if (original.getType() != HitResult.Type.MISS) return original;

        return entity.pick(maxDistance, tickProgress, true);
    }

    // Sprint

    @ModifyExpressionValue(method = "canStartSprinting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;hasForwardImpulse()Z"))
    private boolean modifyIsWalking(boolean original) {
        if (!Modules.get().get(Sprint.class).rageSprint()) return original;

        float forwards = Math.abs(zza);
        float sideways = Math.abs(xxa);

        return (isUnderWater() ? (forwards > 1.0E-5F || sideways > 1.0E-5F) : (forwards > 0.8 || sideways > 0.8));
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;hasForwardImpulse()Z"))
    private boolean modifyAiStep(boolean original) {
        if (!Modules.get().get(Sprint.class).rageSprint()) return original;

        return Math.abs(xxa) > 1.0E-5F || Math.abs(zza) > 1.0E-5F;
    }

    @WrapWithCondition(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;setSprinting(Z)V", ordinal = 3))
    private boolean wrapSetSprinting(LocalPlayer instance, boolean b) {
        Sprint s = Modules.get().get(Sprint.class);

        return !s.rageSprint() || s.unsprintInWater() && isInWater();
    }

    // Rotations

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void onSendPositionHead(CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(SendMovementPacketsEvent.Pre.get());
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V", ordinal = 1))
    private void onTickHasVehicleBeforeSendPackets(CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(SendMovementPacketsEvent.Pre.get());
    }

    @Inject(method = "sendPosition", at = @At("TAIL"))
    private void onSendPositionTail(CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(SendMovementPacketsEvent.Post.get());
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V", ordinal = 1, shift = At.Shift.AFTER))
    private void onTickHasVehicleAfterSendPackets(CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(SendMovementPacketsEvent.Post.get());
    }
}
