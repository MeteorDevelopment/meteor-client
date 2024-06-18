/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.DropItemsEvent;
import meteordevelopment.meteorclient.events.entity.player.*;
import meteordevelopment.meteorclient.mixininterface.IClientPlayerInteractionManager;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.InventoryTweaks;
import meteordevelopment.meteorclient.systems.modules.player.BreakDelay;
import meteordevelopment.meteorclient.systems.modules.player.SpeedMine;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin implements IClientPlayerInteractionManager {
    @Shadow private int blockBreakingCooldown;

    @Shadow protected abstract void syncSelectedSlot();

    @Shadow
    public abstract void clickSlot(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player);

    @Shadow
    @Final
    private ClientPlayNetworkHandler networkHandler;

    @Shadow
    public abstract boolean breakBlock(BlockPos pos);

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

    @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
    public void onClickArmorSlot(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (!Modules.get().get(InventoryTweaks.class).armorStorage()) return;

        ScreenHandler screenHandler = player.currentScreenHandler;

        if (screenHandler instanceof PlayerScreenHandler) {
            if (slotId >= 5 && slotId <= 8) {
                int armorSlot = (8 - slotId) + 36;
                if (actionType == SlotActionType.PICKUP && !screenHandler.getCursorStack().isEmpty()) {
                    clickSlot(syncId, 17, armorSlot, SlotActionType.SWAP, player); //armor slot <-> inv slot
                    clickSlot(syncId, 17, button, SlotActionType.PICKUP, player); //inv slot <-> cursor slot
                    clickSlot(syncId, 17, armorSlot, SlotActionType.SWAP, player); //armor slot <-> inv slot
                    ci.cancel();
                } else if (actionType == SlotActionType.SWAP) {
                    if (button >= 10) {
                        clickSlot(syncId, 45, armorSlot, SlotActionType.SWAP, player);
                        ci.cancel();
                    } else {
                        clickSlot(syncId, 36 + button, armorSlot, SlotActionType.SWAP, player); //invert swap
                        ci.cancel();
                    }
                }
            }
        }
    }

    @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
    private void onAttackBlock(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> info) {
        if (MeteorClient.EVENT_BUS.post(StartBreakingBlockEvent.get(blockPos, direction)).isCancelled()) info.cancel();
        else {
            SpeedMine sm = Modules.get().get(SpeedMine.class);
            BlockState state = mc.world.getBlockState(blockPos);

            if (!sm.instamine() || !sm.filter(state.getBlock())) return;

            if (state.calcBlockBreakingDelta(mc.player, mc.world, blockPos) > 0.5f) {
                breakBlock(blockPos);
                networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
                networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
                info.setReturnValue(true);
            }
        }
    }

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    public void interactBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
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

    @Redirect(method = "updateBlockBreakingProgress", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;blockBreakingCooldown:I", opcode = Opcodes.PUTFIELD, ordinal = 1))
    private void creativeBreakDelayChange(ClientPlayerInteractionManager interactionManager, int value) {
        BlockBreakingCooldownEvent event = MeteorClient.EVENT_BUS.post(BlockBreakingCooldownEvent.get(value));
        blockBreakingCooldown = event.cooldown;
    }

    @Redirect(method = "updateBlockBreakingProgress", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;blockBreakingCooldown:I", opcode = Opcodes.PUTFIELD, ordinal = 2))
    private void survivalBreakDelayChange(ClientPlayerInteractionManager interactionManager, int value) {
        BlockBreakingCooldownEvent event = MeteorClient.EVENT_BUS.post(BlockBreakingCooldownEvent.get(value));
        blockBreakingCooldown = event.cooldown;
    }

    @Redirect(method = "attackBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;blockBreakingCooldown:I", opcode = Opcodes.PUTFIELD))
    private void creativeBreakDelayChange2(ClientPlayerInteractionManager interactionManager, int value) {
        BlockBreakingCooldownEvent event = MeteorClient.EVENT_BUS.post(BlockBreakingCooldownEvent.get(value));
        blockBreakingCooldown = event.cooldown;
    }

    @Redirect(method = "method_41930", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;calcBlockBreakingDelta(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)F"))
    private float deltaChange(BlockState blockState, PlayerEntity player, BlockView world, BlockPos pos) {
        float delta = blockState.calcBlockBreakingDelta(player, world, pos);
        if (Modules.get().get(BreakDelay.class).preventInstaBreak() && delta >= 1) {
            BlockBreakingCooldownEvent event = MeteorClient.EVENT_BUS.post(BlockBreakingCooldownEvent.get(blockBreakingCooldown));
            blockBreakingCooldown = event.cooldown;
            return 0;
        }
        return delta;
    }

    @Inject(method = "breakBlock", at = @At("HEAD"), cancellable = true)
    private void onBreakBlock(BlockPos blockPos, CallbackInfoReturnable<Boolean> info) {
        if (MeteorClient.EVENT_BUS.post(BreakBlockEvent.get(blockPos)).isCancelled()) info.setReturnValue(false);
    }

    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void onInteractItem(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> info) {
        InteractItemEvent event = MeteorClient.EVENT_BUS.post(InteractItemEvent.get(hand));
        if (event.toReturn != null) info.setReturnValue(event.toReturn);
    }

    @Inject(method = "cancelBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void onCancelBlockBreaking(CallbackInfo info) {
        if (BlockUtils.breaking) info.cancel();
    }

    @Override
    public void meteor$syncSelected() {
        syncSelectedSlot();
    }
}
