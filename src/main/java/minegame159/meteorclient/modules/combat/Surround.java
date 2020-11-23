/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.PlayerUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class Surround extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> doubleHeight = sgGeneral.add(new BoolSetting.Builder()
            .name("double-height")
            .description("Places obsidian around your head.")
            .defaultValue(false)
            .build()
    );
    
    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Works only when you standing on ground.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> onlyWhenSneaking = sgGeneral.add(new BoolSetting.Builder()
            .name("only-when-sneaking")
            .description("Places blocks only when sneaking.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> turnOff = sgGeneral.add(new BoolSetting.Builder()
            .name("turn-off")
            .description("Turns off when placed.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> center = sgGeneral.add(new BoolSetting.Builder()
            .name("center")
            .description("Moves you to the center of the block.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableOnJump = sgGeneral.add(new BoolSetting.Builder()
            .name("disable-on-jump")
            .description("Automatically disables when you jump.")
            .defaultValue(true)
            .build()
    );

    private int prevSlot;
    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
    private boolean return_;

    public Surround() {
        super(Category.Combat, "surround", "Surrounds you with obsidian (or other blocks) to take less damage.");
    }

    @Override
    public void onActivate() {
        if (center.get()) {
            double x = MathHelper.floor(mc.player.getX()) + 0.5;
            double z = MathHelper.floor(mc.player.getZ()) + 0.5;
            mc.player.updatePosition(x, mc.player.getY(), z);
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionOnly(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
        }
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (disableOnJump.get() && mc.options.keyJump.isPressed()) {
            toggle();
            return;
        }

        if (onlyOnGround.get() && !mc.player.isOnGround()) return;
        if (onlyWhenSneaking.get() && !mc.options.keySneak.isPressed()) return;

        // Place
        return_ = false;

        // Bottom
        boolean p1 = place(0, -1, 0);
        if (return_) return;

        // Sides
        boolean p2 = place(1, 0, 0);
        if (return_) return;
        boolean p3 = place(-1, 0, 0);
        if (return_) return;
        boolean p4 = place(0, 0, 1);
        if (return_) return;
        boolean p5 = place(0, 0, -1);
        if (return_) return;

        // Sides up
        boolean doubleHeightPlaced = false;
        if (doubleHeight.get()) {
            boolean p6 = place(1, 1, 0);
            if (return_) return;
            boolean p7 = place(-1, 1, 0);
            if (return_) return;
            boolean p8 = place(0, 1, 1);
            if (return_) return;
            boolean p9 = place(0, 1, -1);
            if (return_) return;

            if (p6 && p7 && p8 && p9) doubleHeightPlaced = true;
        }

        // Auto turn off
        if (turnOff.get() && p1 && p2 && p3 && p4 && p5) {
            if (doubleHeightPlaced || !doubleHeight.get()) toggle();
        }
    });

    private boolean place(int x, int y, int z) {
        setBlockPos(x, y, z);

        BlockState blockState = mc.world.getBlockState(blockPos);
        boolean wasObby = blockState.getBlock() == Blocks.OBSIDIAN;
        boolean placed = !blockState.getMaterial().isReplaceable();

        if (!placed && findSlot()) {
            placed = PlayerUtils.placeBlock(blockPos);
            resetSlot();

            boolean isObby = mc.world.getBlockState(blockPos).getBlock() == Blocks.OBSIDIAN;
            if (!wasObby && isObby) return_ = true;
        }

        return placed;
    }

    private void setBlockPos(int x, int y, int z) {
        blockPos.set(mc.player.getX() + x, mc.player.getY() + y, mc.player.getZ() + z);
    }

    private boolean findSlot() {
        prevSlot = mc.player.inventory.selectedSlot;

        for (int i = 0; i < 9; i++) {
            Item item = mc.player.inventory.getStack(i).getItem();

            if (!(item instanceof BlockItem)) continue;

            if (item == Items.OBSIDIAN) {
                mc.player.inventory.selectedSlot = i;
                return true;
            }
        }

        return false;
    }

    private void resetSlot() {
        mc.player.inventory.selectedSlot = prevSlot;
    }
}
