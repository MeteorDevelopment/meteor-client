/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.entity.player.AttackEntityEvent;
import minegame159.meteorclient.events.entity.player.BreakBlockEvent;
import minegame159.meteorclient.events.entity.player.InteractItemEvent;
import minegame159.meteorclient.events.entity.player.StartBreakingBlockEvent;
import minegame159.meteorclient.mixininterface.IClientPlayerInteractionManager;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.misc.Nuker;
import minegame159.meteorclient.modules.player.NoBreakDelay;
import minegame159.meteorclient.modules.player.Reach;
import minegame159.meteorclient.utils.player.Rotations;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin implements IClientPlayerInteractionManager {
    @Shadow private int blockBreakingCooldown;

    @Shadow protected abstract void syncSelectedSlot();

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo info) {
        Rotations.resetLastRotation();

        AttackEntityEvent event = MeteorClient.EVENT_BUS.post(AttackEntityEvent.get(target));

        if (event.isCancelled()) info.cancel();
    }

    @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
    private void onAttackBlock(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> info) {
        Rotations.resetLastRotation();

        StartBreakingBlockEvent event = MeteorClient.EVENT_BUS.post(StartBreakingBlockEvent.get(blockPos, direction));

        if (event.isCancelled()) info.cancel();
    }

    @Inject(method = "getReachDistance", at = @At("HEAD"), cancellable = true)
    private void onGetReachDistance(CallbackInfoReturnable<Float> info) {
        if (Modules.get().isActive(Reach.class)) info.setReturnValue(Modules.get().get(Reach.class).getReach());
    }

    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"))
    private void onUpdateBlockBreakingProgress(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> info) {
        Rotations.resetLastRotation();
    }

    @Redirect(method = "updateBlockBreakingProgress", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;blockBreakingCooldown:I", opcode = Opcodes.PUTFIELD))
    private void onMethod_2902SetField_3716Proxy(ClientPlayerInteractionManager interactionManager, int value) {
        if (Modules.get().isActive(Nuker.class)) value = 0;
        if (Modules.get().isActive(NoBreakDelay.class)) value = 0;
        blockBreakingCooldown = value;
    }

    @Redirect(method = "attackBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;blockBreakingCooldown:I", opcode = Opcodes.PUTFIELD))
    private void onAttackBlockSetField_3719Proxy(ClientPlayerInteractionManager interactionManager, int value) {
        if (Modules.get().isActive(Nuker.class)) value = 0;
        blockBreakingCooldown = value;
    }

    @Inject(method = "breakBlock", at = @At("HEAD"))
    private void onBreakBlock(BlockPos blockPos, CallbackInfoReturnable<Boolean> info) {
        MeteorClient.EVENT_BUS.post(BreakBlockEvent.get(blockPos));
    }

    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void onInteractItem(PlayerEntity player, World world, Hand hand, CallbackInfoReturnable<ActionResult> info) {
        Rotations.resetLastRotation();

        InteractItemEvent event = MeteorClient.EVENT_BUS.post(InteractItemEvent.get(hand));
        if (event.toReturn != null) info.setReturnValue(event.toReturn);
    }

    @Inject(method = "interactBlock", at = @At("HEAD"))
    private void onInteractBlock(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> info) {
        Rotations.resetLastRotation();
    }

    @Inject(method = "interactEntity", at = @At("HEAD"))
    private void onInteractEntity(PlayerEntity player, Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> info) {
        Rotations.resetLastRotation();
    }

    @Inject(method = "interactEntityAtLocation", at = @At("HEAD"))
    private void onInteractEntityAtLocation(PlayerEntity player, Entity entity, EntityHitResult hitResult, Hand hand, CallbackInfoReturnable<ActionResult> info) {
        Rotations.resetLastRotation();
    }

    @Override
    public void syncSelectedSlot2() {
        syncSelectedSlot();
    }
}
