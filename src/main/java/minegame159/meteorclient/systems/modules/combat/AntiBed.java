/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.combat;

//Created by squidoodly 07/08/2020

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.*;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AntiBed extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> autoToggle = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-toggle")
            .description("Toggles AntiBed off when finished.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> autoCenter = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-center")
            .description("Teleports you to the center of the blocks.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Only toggles Anti Bed when you are standing on a block.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates towards where the blocks are placed.")
            .defaultValue(true)
            .build()
    );

    private int place = -1;
    private boolean closeScreen = false;

    public AntiBed() {
        super(Categories.Combat, "anti-bed", "Prevents people from placing beds where you are standing.");
    }

    @Override
    public void onDeactivate() {
        closeScreen = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (closeScreen && mc.currentScreen instanceof SignEditScreen) {
            closeScreen = false;
            mc.player.closeScreen();
            return;
        }
        else if (closeScreen) {
            return;
        }

        if (!mc.world.getBlockState(mc.player.getBlockPos().up()).isAir()) return;
        if (onlyOnGround.get() && !mc.player.isOnGround()) return;

        if (place == 0) {
            place --;
            place(mc.player.inventory.selectedSlot, true);
        }
        else if (place > 0) {
            place --;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.player.inventory.getStack(i);
            Item item = itemStack.getItem();
            Block block = Block.getBlockFromItem(item);

            if (item == Items.STRING
                    || block instanceof TrapdoorBlock
                    || item == Items.COBWEB) {
                place(i, true);
                return;
            }
            else if (block instanceof SlabBlock) {
                mc.player.inventory.selectedSlot = i;
                mc.options.keySneak.setPressed(true);
                if (place == -1) place = 2;
                return;
            }
            else if (block instanceof DoorBlock) {
                if (autoCenter.get()) {
                    Vec3d playerVec = Utils.vec3d(mc.player.getBlockPos());
                    if (mc.player.getHorizontalFacing() == Direction.SOUTH) {
                        playerVec = playerVec.add(0.5, 0, 0.7);
                    } else if (mc.player.getHorizontalFacing() == Direction.NORTH) {
                        playerVec = playerVec.add(0.5, 0, 0.3);
                    } else if (mc.player.getHorizontalFacing() == Direction.EAST) {
                        playerVec = playerVec.add(0.7, 0, 0.5);
                    } else if (mc.player.getHorizontalFacing() == Direction.WEST) {
                        playerVec = playerVec.add(0.3, 0, 0.5);
                    }
                    mc.player.updatePosition(playerVec.x, playerVec.y, playerVec.z);
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionOnly(playerVec.x, playerVec.y, playerVec.z, mc.player.isOnGround()));
                }
                place(i, true);
                return;
            }
            else if (item == Items.LADDER) {
                if (autoCenter.get()) {
                    Vec3d playerVec = Utils.vec3d(mc.player.getBlockPos());
                    BlockPos blockPos = checkBlocks();
                    if (blockPos == null) return;
                    if (playerVec.subtract(Utils.vec3d(blockPos)).x > 0) {
                        playerVec = playerVec.add(0.7, 0, 0.5);
                    } else if (playerVec.subtract(Utils.vec3d(blockPos)).x < 0) {
                        playerVec = playerVec.add(0.3, 0, 0.5);
                    } else if (playerVec.subtract(Utils.vec3d(blockPos)).z > 0) {
                        playerVec = playerVec.add(0.5, 0, 0.7);
                    } else if (playerVec.subtract(Utils.vec3d(blockPos)).z < 0) {
                        playerVec = playerVec.add(0.5, 0, 0.3);
                    }
                    mc.player.updatePosition(playerVec.x, playerVec.y, playerVec.z);
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionOnly(playerVec.x, playerVec.y, playerVec.z, mc.player.isOnGround()));
                }
                place(i, true);
                return;
            }
            else if (item instanceof BannerItem
                    || item == Items.LEVER || item == Items.TORCH
                    || item == Items.REDSTONE_TORCH || item instanceof SignItem
                    || item == Items.TRIPWIRE_HOOK || block instanceof StoneButtonBlock
                    || block instanceof WoodenButtonBlock) {
                place(i, true);
                if (item instanceof SignItem) closeScreen = true;
                return;
            }
            else if (item == Items.SCAFFOLDING && itemStack.getCount() >= 2) {
                place(i, false);
                place(i, true);
                return;
            }
        }
    }

    private BlockPos checkBlocks(){
        BlockPos blockPos = null;
        if (!mc.world.getBlockState(mc.player.getBlockPos().add(0, 1, 1)).isAir()) {
            blockPos = mc.player.getBlockPos().add(0, 1, 1);
        } else if (!mc.world.getBlockState(mc.player.getBlockPos().add(0, 1, -1)).isAir()) {
            blockPos = mc.player.getBlockPos().add(0, 1, -1);
        } else if (!mc.world.getBlockState(mc.player.getBlockPos().add(1, 1, 0)).isAir()) {
            blockPos = mc.player.getBlockPos().add(1, 1, 0);
        } else if (!mc.world.getBlockState(mc.player.getBlockPos().add(-1, 1, 0)).isAir()) {
            blockPos = mc.player.getBlockPos().add(-1, 1, 0);
        }
        return blockPos;
    }

    private void place(int slot, boolean up) {
        BlockPos blockPos;
        if (up) blockPos = mc.player.getBlockPos().up();
        else blockPos = mc.player.getBlockPos();

        if (BlockUtils.place(blockPos, Hand.MAIN_HAND, slot, rotate.get(), 100, true)) {
            if (autoToggle.get()) toggle();
        }
    }
}
