/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.DropItemsEvent;
import meteordevelopment.meteorclient.events.entity.player.*;
import meteordevelopment.meteorclient.mixininterface.IClientPlayerInteractionManager;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.NoBreakDelay;
import meteordevelopment.meteorclient.systems.modules.player.Reach;
import meteordevelopment.meteorclient.systems.modules.world.Nuker;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
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

    @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
    private void onClickSlot(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo info) {
        if (actionType == SlotActionType.THROW && slotId >= 0 && slotId < player.currentScreenHandler.slots.size()) {
            if (MeteorClient.EVENT_BUS.post(DropItemsEvent.get(player.currentScreenHandler.slots.get(slotId).getStack())).isCancelled()) info.cancel();
        }
        else if (slotId == -999) {
            // Clicking outside of inventory
            if (MeteorClient.EVENT_BUS.post(DropItemsEvent.get(player.currentScreenHandler.getCursorStack())).isCancelled()) info.cancel();
        }
    }

    @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
    private void onAttackBlock(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> info) {
        if (MeteorClient.EVENT_BUS.post(StartBreakingBlockEvent.get(blockPos, direction)).isCancelled()) info.cancel();
    }

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    public void interactBlock(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (MeteorClient.EVENT_BUS.post(InteractBlockEvent.get(player.getMainHandStack().isEmpty() ? Hand.OFF_HAND : hand, hitResult)).isCancelled()) cir.setReturnValue(ActionResult.FAIL);
    }

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo info) {
        if (MeteorClient.EVENT_BUS.post(AttackEntityEvent.get(target)).isCancelled()) info.cancel();
    }

    @Inject(method = "interactEntity", at = @At("HEAD"), cancellable = true)
    private void onInteractEntity(PlayerEntity player, Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> info) {
        if (MeteorClient.EVENT_BUS.post(InteractEntityEvent.get(entity, hand)).isCancelled()) info.setReturnValue(ActionResult.FAIL);
    }

    @Inject(method = "dropCreativeStack", at = @At("HEAD"), cancellable = true)
    private void onDropCreativeStack(ItemStack stack, CallbackInfo info) {
        if (MeteorClient.EVENT_BUS.post(DropItemsEvent.get(stack)).isCancelled()) info.cancel();
    }

    @Inject(method = "getReachDistance", at = @At("HEAD"), cancellable = true)
    private void onGetReachDistance(CallbackInfoReturnable<Float> info) {
        info.setReturnValue(Modules.get().get(Reach.class).getReach());
    }

    @Redirect(method = "updateBlockBreakingProgress", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;blockBreakingCooldown:I", opcode = Opcodes.PUTFIELD))
    private void onMethod_2902SetField_3716Proxy(ClientPlayerInteractionManager interactionManager, int value) {
        if (Modules.get().isActive(NoBreakDelay.class) || Modules.get().isActive(Nuker.class)) value = 0;
        blockBreakingCooldown = value;
    }

    @Redirect(method = "attackBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;blockBreakingCooldown:I", opcode = Opcodes.PUTFIELD))
    private void onAttackBlockSetField_3719Proxy(ClientPlayerInteractionManager interactionManager, int value) {
        if (Modules.get().isActive(NoBreakDelay.class) || Modules.get().isActive(Nuker.class)) value = 0;
        blockBreakingCooldown = value;
    }

    @Inject(method = "breakBlock", at = @At("HEAD"))
    private void onBreakBlock(BlockPos blockPos, CallbackInfoReturnable<Boolean> info) {
        MeteorClient.EVENT_BUS.post(BreakBlockEvent.get(blockPos));
    }

    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void onInteractItem(PlayerEntity player, World world, Hand hand, CallbackInfoReturnable<ActionResult> info) {
        InteractItemEvent event = MeteorClient.EVENT_BUS.post(InteractItemEvent.get(hand));
        if (event.toReturn != null) info.setReturnValue(event.toReturn);
    }

    @Inject(method = "cancelBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void onCancelBlockBreaking(CallbackInfo info) {
        if (BlockUtils.breaking) info.cancel();
    }

    @Override
    public void syncSelected() {
        syncSelectedSlot();
    }
}
