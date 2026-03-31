/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * @author seasnail8169
 */
public class Burrow extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Block> block = sgGeneral.add(new EnumSetting.Builder<Block>()
        .name("block-to-use")
        .description("The block to use for Burrow.")
        .defaultValue(Block.EChest)
        .build()
    );

    private final Setting<Boolean> instant = sgGeneral.add(new BoolSetting.Builder()
        .name("instant")
        .description("Jumps with packets rather than vanilla jump.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> automatic = sgGeneral.add(new BoolSetting.Builder()
        .name("automatic")
        .description("Automatically burrows on activate rather than waiting for jump.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> triggerHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("trigger-height")
        .description("How high you have to jump before a rubberband is triggered.")
        .defaultValue(1.12)
        .range(0.01, 1.4)
        .sliderRange(0.01, 1.4)
        .build()
    );

    private final Setting<Double> rubberbandHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("rubberband-height")
        .description("How far to attempt to cause rubberband.")
        .defaultValue(12)
        .sliderMin(-30)
        .sliderMax(30)
        .build()
    );

    private final Setting<Double> timer = sgGeneral.add(new DoubleSetting.Builder()
        .name("timer")
        .description("Timer override.")
        .defaultValue(1)
        .min(0.01)
        .sliderRange(0.01, 10)
        .build()
    );

    private final Setting<Boolean> onlyInHole = sgGeneral.add(new BoolSetting.Builder()
        .name("only-in-holes")
        .description("Stops you from burrowing when not in a hole.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> center = sgGeneral.add(new BoolSetting.Builder()
        .name("center")
        .description("Centers you to the middle of the block before burrowing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Faces the block you place server-side.")
        .defaultValue(true)
        .build()
    );

    private final BlockPos.MutableBlockPos blockPos = new BlockPos.Mutable();
    private boolean shouldBurrow;

    public Burrow() {
        super(Categories.Combat, "burrow", "Attempts to clip you into a block.");
    }

    @Override
    public void onActivate() {
        if (!mc.world.getBlockState(mc.player.getBlockPos()).isReplaceable()) {
            error("Already burrowed, disabling.");
            toggle();
            return;
        }

        if (!PlayerUtils.isInHole(false) && onlyInHole.get()) {
            error("Not in a hole, disabling.");
            toggle();
            return;
        }

        if (!checkHead()) {
            error("Not enough headroom to burrow, disabling.");
            toggle();
            return;
        }

        FindItemResult result = getItem();

        if (!result.isHotbar() && !result.isOffhand()) {
            error("No burrow block found, disabling.");
            toggle();
            return;
        }

        blockPos.set(mc.player.getBlockPos());

        Modules.get().get(Timer.class).setOverride(this.timer.get());

        shouldBurrow = false;

        if (automatic.get()) {
            if (instant.get()) shouldBurrow = true;
            else mc.player.jump();
        } else {
            info("Waiting for manual jump.");
        }
    }

    @Override
    public void onDeactivate() {
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!instant.get()) shouldBurrow = mc.player.getY() > blockPos.getY() + triggerHeight.get();
        if (!shouldBurrow && instant.get()) blockPos.set(mc.player.getBlockPos());

        if (shouldBurrow) {
            if (rotate.get())
                Rotations.rotate(Rotations.getYaw(mc.player.getBlockPos()), Rotations.getPitch(mc.player.getBlockPos()), 50, this::burrow);
            else burrow();

            toggle();
        }
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (instant.get() && !shouldBurrow) {
            if (event.action == KeyAction.Press && mc.options.jumpKey.matchesKey(event.input)) {
                shouldBurrow = true;
            }
            blockPos.set(mc.player.getBlockPos());
        }
    }

    private void burrow() {
        if (center.get()) PlayerUtils.centerPlayer();

        if (instant.get()) {
            mc.player.networkHandler.sendPacket(new ServerboundMovePlayerPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.4, mc.player.getZ(), false, mc.player.horizontalCollision));
            mc.player.networkHandler.sendPacket(new ServerboundMovePlayerPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.75, mc.player.getZ(), false, mc.player.horizontalCollision));
            mc.player.networkHandler.sendPacket(new ServerboundMovePlayerPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.01, mc.player.getZ(), false, mc.player.horizontalCollision));
            mc.player.networkHandler.sendPacket(new ServerboundMovePlayerPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.15, mc.player.getZ(), false, mc.player.horizontalCollision));
        }


        FindItemResult block = getItem();

        if (!(mc.player.getInventory().getStack(block.slot()).getItem() instanceof BlockItem)) return;
        InvUtils.swap(block.slot(), true);

        mc.interactionManager.interactBlock(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Utils.vec3d(blockPos), Direction.UP, blockPos, false));
        mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(InteractionHand.MAIN_HAND));

        InvUtils.swapBack();

        if (instant.get()) {
            mc.player.networkHandler.sendPacket(new ServerboundMovePlayerPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + rubberbandHeight.get(), mc.player.getZ(), false, mc.player.horizontalCollision));
        } else {
            mc.player.updatePosition(mc.player.getX(), mc.player.getY() + rubberbandHeight.get(), mc.player.getZ());
        }
    }

    private FindItemResult getItem() {
        return switch (block.get()) {
            case EChest -> InvUtils.findInHotbar(Items.ENDER_CHEST);
            case Anvil ->
                InvUtils.findInHotbar(itemStack -> net.minecraft.block.Block.getBlockFromItem(itemStack.getItem()) instanceof AnvilBlock);
            case Held ->
                new FindItemResult(mc.player.getInventory().getSelectedSlot(), mc.player.getMainHandStack().getCount());
            default -> InvUtils.findInHotbar(Items.OBSIDIAN, Items.CRYING_OBSIDIAN);
        };
    }

    private boolean checkHead() {
        BlockState blockState1 = mc.world.getBlockState(blockPos.set(mc.player.getX() + .3, mc.player.getY() + 2.3, mc.player.getZ() + .3));
        BlockState blockState2 = mc.world.getBlockState(blockPos.set(mc.player.getX() + .3, mc.player.getY() + 2.3, mc.player.getZ() - .3));
        BlockState blockState3 = mc.world.getBlockState(blockPos.set(mc.player.getX() - .3, mc.player.getY() + 2.3, mc.player.getZ() - .3));
        BlockState blockState4 = mc.world.getBlockState(blockPos.set(mc.player.getX() - .3, mc.player.getY() + 2.3, mc.player.getZ() + .3));
        boolean air1 = blockState1.isReplaceable();
        boolean air2 = blockState2.isReplaceable();
        boolean air3 = blockState3.isReplaceable();
        boolean air4 = blockState4.isReplaceable();
        return air1 && air2 && air3 && air4;
    }

    public enum Block {
        EChest,
        Obsidian,
        Anvil,
        Held
    }
}
