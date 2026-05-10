/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.DropItemsEvent;
import meteordevelopment.meteorclient.events.entity.player.*;
import meteordevelopment.meteorclient.mixininterface.IMultiPlayerGameMode;
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
public abstract class MultiPlayerGameModeMixin implements IMultiPlayerGameMode {
    @Shadow
    private int destroyDelay;

    @Shadow
    protected abstract void ensureHasSentCarriedItem();

    @Shadow
    public abstract boolean destroyBlock(BlockPos pos);

    @Shadow
    public abstract void startPrediction(ClientLevel level, PredictiveAction predictiveAction);

    @Inject(method = "handleContainerInput", at = @At("HEAD"), cancellable = true)
    private void onHandleInventoryMouseClick(int containerId, int slotNum, int buttonNum, ContainerInput containerInput, Player player, CallbackInfo ci) {
        if (containerInput == ContainerInput.THROW && slotNum >= 0 && slotNum < player.containerMenu.slots.size()) {
            if (MeteorClient.EVENT_BUS.post(DropItemsEvent.get(player.containerMenu.slots.get(slotNum).getItem())).isCancelled())
                ci.cancel();
        } else if (slotNum == -999) {
            // Clicking outside of inventory
            if (MeteorClient.EVENT_BUS.post(DropItemsEvent.get(player.containerMenu.getCarried())).isCancelled())
                ci.cancel();
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onStartDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (MeteorClient.EVENT_BUS.post(StartBreakingBlockEvent.get(pos, direction)).isCancelled()) cir.cancel();
        else {
            SpeedMine sm = Modules.get().get(SpeedMine.class);
            BlockState state = mc.level.getBlockState(pos);

            if (!sm.instamine() || !sm.filter(state.getBlock())) return;

            if (state.getDestroyProgress(mc.player, mc.level, pos) > 0.5f) {
                destroyBlock(pos);
                startPrediction(mc.level, sequence -> new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, direction, sequence));
                startPrediction(mc.level, sequence -> new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, direction, sequence));
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    public void useItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult blockHit, CallbackInfoReturnable<InteractionResult> cir) {
        if (MeteorClient.EVENT_BUS.post(InteractBlockEvent.get(player.getMainHandItem().isEmpty() ? InteractionHand.OFF_HAND : hand, blockHit)).isCancelled())
            cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Player player, Entity entity, CallbackInfo ci) {
        if (MeteorClient.EVENT_BUS.post(AttackEntityEvent.get(entity)).isCancelled()) ci.cancel();
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void onInteract(Player player, Entity entity, EntityHitResult hitResult, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (MeteorClient.EVENT_BUS.post(InteractEntityEvent.get(entity, hand)).isCancelled())
            cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "handleCreativeModeItemDrop", at = @At("HEAD"), cancellable = true)
    private void onHandleCreativeModeItemDrop(ItemStack clicked, CallbackInfo ci) {
        if (MeteorClient.EVENT_BUS.post(DropItemsEvent.get(clicked)).isCancelled()) ci.cancel();
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
    private void onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (MeteorClient.EVENT_BUS.post(BreakBlockEvent.get(pos)).isCancelled()) cir.setReturnValue(false);
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void onUseItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        InteractItemEvent event = MeteorClient.EVENT_BUS.post(InteractItemEvent.get(hand));
        if (event.toReturn != null) cir.setReturnValue(event.toReturn);
    }

    @Inject(method = "stopDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onStopDestroyBlock(CallbackInfo ci) {
        if (BlockUtils.breaking) ci.cancel();
    }

    @Override
    public void meteor$syncSelected() {
        ensureHasSentCarriedItem();
    }
}
