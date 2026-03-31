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
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
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

@Mixin(AbstractClientPlayer.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayer {
    @Shadow
    public ClientInput input;

    public ClientPlayerEntityMixin(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void onDropSelectedItem(boolean dropEntireStack, CallbackInfoReturnable<Boolean> info) {
        if (MeteorClient.EVENT_BUS.post(DropItemsEvent.get(getMainHandStack())).isCancelled())
            info.setReturnValue(false);
    }

    @ModifyExpressionValue(method = "tickNausea", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;", opcode = Opcodes.GETFIELD))
    private Screen modifyNauseaCurrentScreen(Screen original) {
        if (Modules.get().isActive(Portals.class)) return null;
        return original;
    }

    @ModifyExpressionValue(method = "applyMovementSpeedFactors", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean redirectUsingItem(boolean isUsingItem) {
        if (Modules.get().get(NoSlow.class).items()) return false;
        return isUsingItem;
    }

    @Inject(method = "isSneaking", at = @At("HEAD"), cancellable = true)
    private void onIsSneaking(CallbackInfoReturnable<Boolean> info) {
        if (Modules.get().get(Scaffold.class).scaffolding()) info.setReturnValue(false);
        if (Modules.get().get(Flight.class).noSneak()) info.setReturnValue(false);
    }

    @Inject(method = "shouldSlowDown", at = @At("HEAD"), cancellable = true)
    private void onShouldSlowDown(CallbackInfoReturnable<Boolean> info) {
        if (Modules.get().get(NoSlow.class).sneaking()) {
            info.setReturnValue(isCrawling());
        }
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void onPushOutOfBlocks(double x, double d, CallbackInfo info) {
        Velocity velocity = Modules.get().get(Velocity.class);
        if (velocity.isActive() && velocity.blocks.get()) {
            info.cancel();
        }
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/ClientInput;keyPresses:Lnet/minecraft/world/entity/player/Input;", opcode = Opcodes.GETFIELD))
    private Input isSneaking(Input original) {
        if (Modules.get().get(Sneak.class).doPacket() || Modules.get().get(NoSlow.class).airStrict()) {
            return new PlayerInput(
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

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void preTickMovement(CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(PlayerTickMovementEvent.get());
    }

    @ModifyReturnValue(method = "getMountJumpStrength", at = @At("RETURN"))
    private float modifyMountJumpStrength(float original) {
        if (Modules.get().get(EntityControl.class).maxJump()) return 1f;
        return original;
    }

    @Inject(method = "getJumpingMount", at = @At("RETURN"), cancellable = true)
    private void changeJumpingMount(CallbackInfoReturnable<PlayerRideableJumping> info) {
        if (Modules.get().get(EntityControl.class).cancelJump()) info.setReturnValue(null);
    }

    // TODO(Ravel): target method getCrosshairTarget with the signature not found
    @ModifyReturnValue(method = "getCrosshairTarget(Lnet/minecraft/entity/Entity;DDF)Lnet/minecraft/util/hit/HitResult;", at = @At("RETURN"))
    private static EntityHitResult onUpdateTargetedEntity(EntityHitResult original, @Local EntityHitResult hitResult) {
        if (original instanceof EntityHitResult ehr) {
            if (Modules.get().get(NoMiningTrace.class).canWork(ehr.getEntity()) && hitResult.getType() == EntityHitResult.Type.BLOCK) {
                return hitResult;
            } else if (ehr.getEntity() instanceof FakePlayerEntity fakePlayer && fakePlayer.noHit) {
                return hitResult;
            }
        }

        return original;
    }

    // TODO(Ravel): target method getCrosshairTarget with the signature not found
    @ModifyExpressionValue(method = "getCrosshairTarget(Lnet/minecraft/entity/Entity;DDF)Lnet/minecraft/util/hit/HitResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;"))
    private static EntityHitResult modifyRaycastResult(EntityHitResult original, Entity entity, double blockInteractionRange, double entityInteractionRange, float tickProgress, @Local(ordinal = 0, argsOnly = true) double maxDistance) {
        if (!Modules.get().isActive(LiquidInteract.class)) return original;
        if (original.getType() != EntityHitResult.Type.MISS) return original;

        return entity.raycast(maxDistance, tickProgress, true);
    }

    // Sprint

    @ModifyExpressionValue(method = "canStartSprinting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;hasForwardImpulse()Z"))
    private boolean modifyIsWalking(boolean original) {
        if (!Modules.get().get(Sprint.class).rageSprint()) return original;

        float forwards = Math.abs(forwardSpeed);
        float sideways = Math.abs(sidewaysSpeed);

        return (isSubmergedInWater() ? (forwards > 1.0E-5F || sideways > 1.0E-5F) : (forwards > 0.8 || sideways > 0.8));
    }

    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;hasForwardImpulse()Z"))
    private boolean modifyMovement(boolean original) {
        if (!Modules.get().get(Sprint.class).rageSprint()) return original;

        return Math.abs(sidewaysSpeed) > 1.0E-5F || Math.abs(forwardSpeed) > 1.0E-5F;
    }

    @WrapWithCondition(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;setSprinting(Z)V", ordinal = 3))
    private boolean wrapSetSprinting(AbstractClientPlayer instance, boolean b) {
        Sprint s = Modules.get().get(Sprint.class);

        return !s.rageSprint() || s.unsprintInWater() && isTouchingWater();
    }

    // Rotations

    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void onSendMovementPacketsHead(CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(SendMovementPacketsEvent.Pre.get());
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;sendPacket(Lnet/minecraft/network/protocol/Packet;)V", ordinal = 1))
    private void onTickHasVehicleBeforeSendPackets(CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(SendMovementPacketsEvent.Pre.get());
    }

    @Inject(method = "sendMovementPackets", at = @At("TAIL"))
    private void onSendMovementPacketsTail(CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(SendMovementPacketsEvent.Post.get());
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;sendPacket(Lnet/minecraft/network/protocol/Packet;)V", ordinal = 1, shift = At.Shift.AFTER))
    private void onTickHasVehicleAfterSendPackets(CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(SendMovementPacketsEvent.Post.get());
    }
}
