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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

// TODO(Ravel): can not resolve target class ClientPlayerInteractionManager
// TODO(Ravel): can not resolve target class ClientPlayerInteractionManager
@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin implements IClientPlayerInteractionManager {
    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    private int blockBreakingCooldown;

    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    protected abstract void syncSelectedSlot();

    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    public abstract boolean breakBlock(BlockPos pos);

    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    public abstract void sendSequencedPacket(ClientLevel world, PredictiveAction packetCreator);

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
    private void onClickSlot(int syncId, int slotId, int button, ClickType actionType, LocalPlayer player, CallbackInfo info) {
        if (actionType == ClickType.THROW && slotId >= 0 && slotId < player.currentScreenHandler.slots.size()) {
            if (MeteorClient.EVENT_BUS.post(DropItemsEvent.get(player.currentScreenHandler.slots.get(slotId).getStack())).isCancelled())
                info.cancel();
        } else if (slotId == -999) {
            // Clicking outside of inventory
            if (MeteorClient.EVENT_BUS.post(DropItemsEvent.get(player.currentScreenHandler.getCursorStack())).isCancelled())
                info.cancel();
        }
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
    private void onAttackBlock(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> info) {
        if (MeteorClient.EVENT_BUS.post(StartBreakingBlockEvent.get(blockPos, direction)).isCancelled()) info.cancel();
        else {
            SpeedMine sm = Modules.get().get(SpeedMine.class);
            BlockState state = mc.world.getBlockState(blockPos);

            if (!sm.instamine() || !sm.filter(state.getBlock())) return;

            if (state.calcBlockBreakingDelta(mc.player, mc.world, blockPos) > 0.5f) {
                breakBlock(blockPos);
                sendSequencedPacket(mc.world, (sequence) -> new PlayerActionC2SPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, blockPos, direction, sequence));
                sendSequencedPacket(mc.world, (sequence) -> new PlayerActionC2SPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction, sequence));
                info.setReturnValue(true);
            }
        }
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    public void interactBlock(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (MeteorClient.EVENT_BUS.post(InteractBlockEvent.get(player.getMainHandStack().isEmpty() ? InteractionHand.OFF_HAND : hand, hitResult)).isCancelled())
            cir.setReturnValue(InteractionResult.FAIL);
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void onAttackEntity(LocalPlayer player, LocalPlayer target, CallbackInfo info) {
        if (MeteorClient.EVENT_BUS.post(AttackEntityEvent.get(target)).isCancelled()) info.cancel();
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "interactEntity", at = @At("HEAD"), cancellable = true)
    private void onInteractEntity(LocalPlayer player, LocalPlayer entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> info) {
        if (MeteorClient.EVENT_BUS.post(InteractEntityEvent.get(entity, hand)).isCancelled())
            info.setReturnValue(InteractionResult.FAIL);
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "dropCreativeStack", at = @At("HEAD"), cancellable = true)
    private void onDropCreativeStack(ItemStack stack, CallbackInfo info) {
        if (MeteorClient.EVENT_BUS.post(DropItemsEvent.get(stack)).isCancelled()) info.cancel();
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Redirect(method = "updateBlockBreakingProgress", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", opcode = Opcodes.PUTFIELD, ordinal = 1))
    private void creativeBreakDelayChange(ClientPlayerInteractionManager interactionManager, int value) {
        BlockBreakingCooldownEvent event = MeteorClient.EVENT_BUS.post(BlockBreakingCooldownEvent.get(value));
        blockBreakingCooldown = event.cooldown;
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Redirect(method = "updateBlockBreakingProgress", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", opcode = Opcodes.PUTFIELD, ordinal = 2))
    private void survivalBreakDelayChange(ClientPlayerInteractionManager interactionManager, int value) {
        BlockBreakingCooldownEvent event = MeteorClient.EVENT_BUS.post(BlockBreakingCooldownEvent.get(value));
        blockBreakingCooldown = event.cooldown;
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Redirect(method = "attackBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", opcode = Opcodes.PUTFIELD))
    private void creativeBreakDelayChange2(ClientPlayerInteractionManager interactionManager, int value) {
        BlockBreakingCooldownEvent event = MeteorClient.EVENT_BUS.post(BlockBreakingCooldownEvent.get(value));
        blockBreakingCooldown = event.cooldown;
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @ModifyExpressionValue(method = "method_41930", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;calcBlockBreakingDelta(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"))
    private float modifyBlockBreakingDelta(float original) {
        if (Modules.get().get(BreakDelay.class).preventInstaBreak() && original >= 1) {
            BlockBreakingCooldownEvent event = MeteorClient.EVENT_BUS.post(BlockBreakingCooldownEvent.get(blockBreakingCooldown));
            blockBreakingCooldown = event.cooldown;
            return 0;
        }
        return original;
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "breakBlock", at = @At("HEAD"), cancellable = true)
    private void onBreakBlock(BlockPos blockPos, CallbackInfoReturnable<Boolean> info) {
        if (MeteorClient.EVENT_BUS.post(BreakBlockEvent.get(blockPos)).isCancelled()) info.setReturnValue(false);
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void onInteractItem(LocalPlayer player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> info) {
        InteractItemEvent event = MeteorClient.EVENT_BUS.post(InteractItemEvent.get(hand));
        if (event.toReturn != null) info.setReturnValue(event.toReturn);
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "cancelBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void onCancelBlockBreaking(CallbackInfo info) {
        if (BlockUtils.breaking) info.cancel();
    }

    @Override
    public void meteor$syncSelected() {
        syncSelectedSlot();
    }
}
