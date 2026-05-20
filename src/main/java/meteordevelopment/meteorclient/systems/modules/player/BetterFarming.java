/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.ServerboundMovePlayerPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IServerboundMovePlayerPacket;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;

public class BetterFarming extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Boolean> cropReplace = sgGeneral.add(new BoolSetting.Builder()
        .name("crop-replace")
        .description("Automatically plant new crop on harvest.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> suppressTrample = sgGeneral.add(new BoolSetting.Builder()
        .name("suppress-trample")
        .description("Attempts to prevent player from trampling crops.")
        .defaultValue(true)
        .build()
    );

    public BetterFarming() {
        super(Categories.Player, "better-farming", "Improvements for crop farming.");
    }

    private Item placeItem = null;
    private ArrayList<BlockPos> placements = new ArrayList<>();
    private int mineCooldown = 0;

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Pre event) {
        if (cropReplace.get()) autoPlaceTick();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onStartBreakingBlockEvent(StartBreakingBlockEvent event) {
        if (cropReplace.get()) autoPlaceBreakEvent(event);
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (suppressTrample.get()) trampleSuppressionSendEvent(event);
    }

    private void autoPlaceTick() {
        if (mineCooldown > 0) {
            mineCooldown--;
        }

        if (placeItem == null) return;
        if (placements.isEmpty()) {
            placeItem = null;
            return;
        }

        BlockPos blockPos = placements.getFirst();
        placements.removeFirst();

        FindItemResult foundItem = InvUtils.find(placeItem);

        // If the found item is not in mainhand yet, its not ready
        if (!foundItem.isMainHand()) return;

        BlockState blockState = mc.level.getBlockState(blockPos);

        if (blockState.is(BlockTags.AIR)) {
            BlockUtils.place(blockPos, foundItem, 0);
        } else {
            BlockUtils.breakBlock(blockPos, true);
        }
    }

    private void autoPlaceBreakEvent(StartBreakingBlockEvent event) {
        MeteorClient.LOG.info("BREAK");
        BlockState blockState = mc.level.getBlockState(event.blockPos);

        if (!blockState.is(BlockTags.CROPS)) return;

        if (mineCooldown > 0) {
            event.cancel();
            return;
        }

        Item blockItem = blockState.getBlock().asItem();
        FindItemResult foundItem = InvUtils.find(blockItem);

        if (!foundItem.found()) return;

        placeItem = blockItem;
        placements.add(event.blockPos);
        mineCooldown = 3;

        if (foundItem.isMainHand()) return;

        mc.gameMode.handlePickItemFromBlock(event.blockPos, false);

        // Cancel the event to allow pickblock to do its thing
        event.cancel();
    }

    private void trampleSuppressionSendEvent(PacketEvent.Send event) {
        if (mc.player == null) return;
        if (!(event.packet instanceof ServerboundMovePlayerPacket)
            || ((IServerboundMovePlayerPacket) event.packet).meteor$getTag() == 1337) return;

        ServerboundMovePlayerPacket packet = (ServerboundMovePlayerPacket) event.packet;

        // Only suppress fall if current block or block beneath is farmland
        BlockPos blockPos = new BlockPos(
            (int) packet.getX(0d),
            (int) packet.getY(0d),
            (int) packet.getZ(0d)
        );

        BlockState blockState1 = mc.level.getBlockState(blockPos.offset(0, -1, 0));
        // Check block one over as well to fix edge jumping
        BlockState blockState2 = mc.level.getBlockState(blockPos.offset(-1, -1, 1));

        MeteorClient.LOG.info(blockPos.toShortString());

        if (blockState1.is(Blocks.FARMLAND) || blockState2.is(Blocks.FARMLAND)) {
            ((ServerboundMovePlayerPacketAccessor) event.packet).meteor$setOnGround(true);
        }
    }
}
