/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.EntityMoveEvent;
import meteordevelopment.meteorclient.events.entity.player.JumpVelocityMultiplierEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.mixininterface.ICamera;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.Hitboxes;
import meteordevelopment.meteorclient.systems.modules.movement.*;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly;
import meteordevelopment.meteorclient.systems.modules.render.ESP;
import meteordevelopment.meteorclient.systems.modules.render.FreeLook;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.systems.modules.world.HighwayBuilder;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @ModifyExpressionValue(method = "updateFluidHeightAndDoFluidPushing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;getFlow(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 updateFluidHeightAndDoFluidPushingFluidStateGetVelocity(Vec3 vec) {
        if ((Object) this != mc.player) return vec;

        Velocity velocity = Modules.get().get(Velocity.class);
        if (velocity.isActive() && velocity.liquids.get()) {
            vec = vec.multiply(velocity.getHorizontal(velocity.liquidsHorizontal), velocity.getVertical(velocity.liquidsVertical), velocity.getHorizontal(velocity.liquidsHorizontal));
        }

        return vec;
    }

    @Inject(method = "isInWater", at = @At(value = "HEAD"), cancellable = true)
    private void isInWater(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this != mc.player) return;

        if (Modules.get().get(Flight.class).isActive()) cir.setReturnValue(false);
        if (Modules.get().get(NoSlow.class).fluidDrag()) cir.setReturnValue(false);
    }

    @Inject(method = "isInLava", at = @At(value = "HEAD"), cancellable = true)
    private void isInLava(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this != mc.player) return;

        if (Modules.get().get(Flight.class).isActive()) cir.setReturnValue(false);
        if (Modules.get().get(NoSlow.class).fluidDrag()) cir.setReturnValue(false);
    }

    @Inject(method = "onAboveBubbleColumn", at = @At("HEAD"))
    private void onAboveBubbleColumn(CallbackInfo ci) {
        if ((Object) this != mc.player) return;

        Jesus jesus = Modules.get().get(Jesus.class);
        if (jesus.isActive()) {
            jesus.isInBubbleColumn = true;
        }
    }

    @Inject(method = "onInsideBubbleColumn", at = @At("HEAD"))
    private void onInsideBubbleColumn(CallbackInfo ci) {
        if ((Object) this != mc.player) return;

        Jesus jesus = Modules.get().get(Jesus.class);
        if (jesus.isActive()) {
            jesus.isInBubbleColumn = true;
        }
    }

    @ModifyExpressionValue(method = "updateSwimming", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isUnderWater()Z"))
    private boolean isSubmergedInWater(boolean submerged) {
        if ((Object) this != mc.player) return submerged;

        if (Modules.get().get(NoSlow.class).fluidDrag()) return false;
        if (Modules.get().get(Flight.class).isActive()) return false;
        return submerged;
    }

    @ModifyArgs(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(DDD)V"))
    private void onPushAwayFrom(Args args, Entity entity) {
        Velocity velocity = Modules.get().get(Velocity.class);

        // Velocity
        if ((Object) this == mc.player && velocity.isActive() && velocity.entityPush.get()) {
            double multiplier = velocity.entityPushAmount.get();
            args.set(0, (double) args.get(0) * multiplier);
            args.set(2, (double) args.get(2) * multiplier);
        }
        // FakePlayerEntity
        else if (entity instanceof FakePlayerEntity player && player.doNotPush) {
            args.set(0, 0.0);
            args.set(2, 0.0);
        }
    }

    @ModifyReturnValue(method = "getBlockJumpFactor", at = @At("RETURN"))
    private float onGetBlockJumpFactor(float original) {
        if ((Object) this == mc.player) {
            JumpVelocityMultiplierEvent event = MeteorClient.EVENT_BUS.post(JumpVelocityMultiplierEvent.get());
            return (original * event.multiplier);
        }

        return original;
    }

    @Inject(method = "move", at = @At("HEAD"))
    private void onMove(MoverType moverType, Vec3 delta, CallbackInfo ci) {
        if ((Object) this == mc.player) {
            MeteorClient.EVENT_BUS.post(PlayerMoveEvent.get(moverType, delta));
        } else {
            MeteorClient.EVENT_BUS.post(EntityMoveEvent.get((Entity) (Object) this, delta));
        }
    }

    @ModifyExpressionValue(method = "getBlockSpeedFactor", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;"))
    private Block modifyBlockSpeedFactor(Block original) {
        if ((Object) this != mc.player) return original;

        if (original == Blocks.SOUL_SAND && Modules.get().get(NoSlow.class).soulSand()) return Blocks.STONE;
        if (original == Blocks.HONEY_BLOCK && Modules.get().get(NoSlow.class).honeyBlock()) return Blocks.STONE;
        return original;
    }

    @ModifyReturnValue(method = "isInvisibleTo", at = @At("RETURN"))
    private boolean isInvisibleToCanceller(boolean original) {
        if (!Utils.canUpdate()) return original;
        ESP esp = Modules.get().get(ESP.class);
        if (Modules.get().get(NoRender.class).noInvisibility() || esp.isActive() && !esp.shouldSkip((Entity) (Object) this))
            return false;
        return original;
    }

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void isCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (Modules.get().get(NoRender.class).noGlowing()) cir.setReturnValue(false);
    }

    @Inject(method = "getPickRadius", at = @At("HEAD"), cancellable = true)
    private void onGetPickRadius(CallbackInfoReturnable<Float> cir) {
        double v = Modules.get().get(Hitboxes.class).getEntityValue((Entity) (Object) this);
        if (v != 0) cir.setReturnValue((float) v);
    }

    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void onIsInvisibleTo(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (player == null) cir.setReturnValue(false);
    }

    @Inject(method = "getPose", at = @At("HEAD"), cancellable = true)
    private void getPoseHook(CallbackInfoReturnable<Pose> cir) {
        if ((Object) this != mc.player) return;

        if (Modules.get().get(ElytraFly.class).canPacketEfly()) {
            cir.setReturnValue(Pose.FALL_FLYING);
        }
    }

    @ModifyReturnValue(method = "getPose", at = @At("RETURN"))
    private Pose modifyGetPose(Pose original) {
        if ((Object) this != mc.player) return original;

        if (original == Pose.CROUCHING && !mc.player.isShiftKeyDown() && ((PlayerAccessor) mc.player).meteor$canChangeIntoPose(Pose.STANDING))
            return Pose.STANDING;
        return original;
    }

    @ModifyReturnValue(method = "isSuppressingBounce", at = @At("RETURN"))
    private boolean cancelBounce(boolean original) {
        return Modules.get().get(NoFall.class).cancelBounce() || original;
    }

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void updateTurn(double xo, double yo, CallbackInfo ci) {
        if ((Object) this != mc.player) return;

        Freecam freecam = Modules.get().get(Freecam.class);
        FreeLook freeLook = Modules.get().get(FreeLook.class);

        if (freecam.isActive()) {
            freecam.changeLookDirection(xo * 0.15, yo * 0.15);
            ci.cancel();
        } else if (Modules.get().isActive(HighwayBuilder.class)) {
            Camera camera = mc.gameRenderer.getMainCamera();
            ((ICamera) camera).meteor$setRot(camera.yRot() + xo * 0.15, camera.xRot() + yo * 0.15);
            ci.cancel();
        } else if (freeLook.cameraMode()) {
            freeLook.cameraYaw += (float) (xo / freeLook.sensitivity.get().floatValue());
            freeLook.cameraPitch += (float) (yo / freeLook.sensitivity.get().floatValue());

            if (Math.abs(freeLook.cameraPitch) > 90.0F)
                freeLook.cameraPitch = freeLook.cameraPitch > 0.0F ? 90.0F : -90.0F;
            ci.cancel();
        }
    }
}
