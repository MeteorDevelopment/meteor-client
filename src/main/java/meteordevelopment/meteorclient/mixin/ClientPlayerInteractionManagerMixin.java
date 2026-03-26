/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.DropItemsEvent;
import meteordevelopment.meteorclient.events.entity.player.*;
import meteordevelopment.meteorclient.mixininterface.IClientPlayerInteractionManager;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.BreakDelay;
import meteordevelopment.meteorclient.systems.modules.player.SpeedMine;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(MultiPlayerGameMode.class)
public abstract class ClientPlayerInteractionManagerMixin implements IClientPlayerInteractionManager {
    @Shadow private int destroyDelay;

    @Shadow protected abstract void ensureHasSentCarriedItem();

    @Shadow
    public abstract boolean destroyBlock(BlockPos pos);

    @Shadow
    public abstract void startPrediction(ClientLevel world, PredictiveAction packetCreator);

    @Inject(method = "handleContainerInput", at = @At("HEAD"), cancellable = true)
    private void onClickSlot(int syncId, int slotId, int button, ContainerInput actionType, Player player, CallbackInfo info) {
        if (actionType == ContainerInput.THROW && slotId >= 0 && slotId < player.containerMenu.slots.size()) {
            if (MeteorClient.EVENT_BUS.post(DropItemsEvent.get(player.containerMenu.slots.get(slotId).getItem())).isCancelled()) info.cancel();
        }
        else if (slotId == -999) {
            // Clicking outside of inventory
            if (MeteorClient.EVENT_BUS.post(DropItemsEvent.get(player.containerMenu.getCarried())).isCancelled()) info.cancel();
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onAttackBlock(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> info) {
        if (MeteorClient.EVENT_BUS.post(StartBreakingBlockEvent.get(blockPos, direction)).isCancelled()) info.cancel();
        else {
            SpeedMine sm = Modules.get().get(SpeedMine.class);
            BlockState state = mc.level.getBlockState(blockPos);

            if (!sm.instamine() || !sm.filter(state.getBlock())) return;

            if (state.getDestroyProgress(mc.player, mc.level, blockPos) > 0.5f) {
                destroyBlock(blockPos);
                startPrediction(mc.level, (sequence) -> new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, blockPos, direction, sequence));
                startPrediction(mc.level, (sequence) -> new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction, sequence));
                info.setReturnValue(true);
            }
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    public void interactBlock(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (MeteorClient.EVENT_BUS.post(InteractBlockEvent.get(player.getMainHandItem().isEmpty() ? InteractionHand.OFF_HAND : hand, hitResult)).isCancelled()) cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttackEntity(Player player, Entity target, CallbackInfo info) {
        if (MeteorClient.EVENT_BUS.post(AttackEntityEvent.get(target)).isCancelled()) info.cancel();
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void onInteractEntity(Player player, Entity entity, EntityHitResult hitResult, InteractionHand hand, CallbackInfoReturnable<InteractionResult> info) {
        if (MeteorClient.EVENT_BUS.post(InteractEntityEvent.get(entity, hand)).isCancelled()) info.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "handleCreativeModeItemDrop", at = @At("HEAD"), cancellable = true)
    private void onDropCreativeStack(ItemStack stack, CallbackInfo info) {
        if (MeteorClient.EVENT_BUS.post(DropItemsEvent.get(stack)).isCancelled()) info.cancel();
    }

    @Redirect(method = "continueDestroyBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", opcode = Opcodes.PUTFIELD, ordinal = 1))
    private void creativeBreakDelayChange(MultiPlayerGameMode interactionManager, int value) {
        BlockBreakingCooldownEvent event = MeteorClient.EVENT_BUS.post(BlockBreakingCooldownEvent.get(value));
        destroyDelay = event.cooldown;
    }

    @Redirect(method = "continueDestroyBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", opcode = Opcodes.PUTFIELD, ordinal = 2))
    private void survivalBreakDelayChange(MultiPlayerGameMode interactionManager, int value) {
        BlockBreakingCooldownEvent event = MeteorClient.EVENT_BUS.post(BlockBreakingCooldownEvent.get(value));
        destroyDelay = event.cooldown;
    }

    @Redirect(method = "startDestroyBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", opcode = Opcodes.PUTFIELD))
    private void creativeBreakDelayChange2(MultiPlayerGameMode interactionManager, int value) {
        BlockBreakingCooldownEvent event = MeteorClient.EVENT_BUS.post(BlockBreakingCooldownEvent.get(value));
        destroyDelay = event.cooldown;
    }

    @ModifyExpressionValue(method = "continueDestroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroyProgress(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"))
    private float modifyBlockBreakingDelta(float original) {
        if (Modules.get().get(BreakDelay.class).preventInstaBreak() && original >= 1) {
            BlockBreakingCooldownEvent event = MeteorClient.EVENT_BUS.post(BlockBreakingCooldownEvent.get(destroyDelay));
            destroyDelay = event.cooldown;
            return 0;
        }
        return original;
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void onBreakBlock(BlockPos blockPos, CallbackInfoReturnable<Boolean> info) {
        if (MeteorClient.EVENT_BUS.post(BreakBlockEvent.get(blockPos)).isCancelled()) info.setReturnValue(false);
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void onInteractItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> info) {
        InteractItemEvent event = MeteorClient.EVENT_BUS.post(InteractItemEvent.get(hand));
        if (event.toReturn != null) info.setReturnValue(event.toReturn);
    }

    @Inject(method = "stopDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onCancelBlockBreaking(CallbackInfo info) {
        if (BlockUtils.breaking) info.cancel();
    }

    @Override
    public void meteor$syncSelected() {
        ensureHasSentCarriedItem();
    }
}
