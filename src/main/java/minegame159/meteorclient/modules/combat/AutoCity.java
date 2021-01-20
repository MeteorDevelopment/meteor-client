/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.player.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class AutoCity extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> checkBelow = sgGeneral.add(new BoolSetting.Builder()
            .name("check-below")
            .description("Checks for obsidian or bedrock below the surround block for you to place crystals on.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> support = sgGeneral.add(new BoolSetting.Builder()
            .name("support")
            .description("If there is no block below a city block it will place one before mining.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> chatInfo = sgGeneral.add(new BoolSetting.Builder()
            .name("chat-info")
            .description("Sends a client-side message if you city a player.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates you towards the city block.")
            .defaultValue(true)
            .build()
    );

    public AutoCity() {
        super(Category.Combat, "auto-city", "Automatically cities a target by mining the nearest obsidian next to them.");
    }

    private PlayerEntity target;

    @Override
    public void onActivate() {
        target = CityUtils.getPlayerTarget();
        BlockPos mineTarget = CityUtils.getTargetBlock(checkBelow.get());

        if (target == null || mineTarget == null) {
            if (chatInfo.get()) ChatUtils.moduleError(this, "No target block found... disabling.");
        } else {
            if (chatInfo.get()) ChatUtils.moduleInfo(this, "Attempting to city " + target.getGameProfile().getName());

            if (MathHelper.sqrt(mc.player.squaredDistanceTo(mineTarget.getX(), mineTarget.getY(), mineTarget.getZ())) > mc.interactionManager.getReachDistance()) {
                if (chatInfo.get()) ChatUtils.moduleError(this, "Target block out of reach... disabling.");
                toggle();
                return;
            }

            int pickSlot = -1;
            for (int i = 0; i < 9; i++) {
                Item item = mc.player.inventory.getStack(i).getItem();

                if (item == Items.DIAMOND_PICKAXE || item == Items.NETHERITE_PICKAXE) {
                    pickSlot = i;
                    break;
                }
            }

            if (pickSlot == -1) {
                if (chatInfo.get()) ChatUtils.moduleError(this, "No pick found... disabling.");
                toggle();
                return;
            }

            int obbySlot = -1;
            for (int i = 0; i < 9; i++) {
                Item item = mc.player.inventory.getStack(i).getItem();

                if (item == Items.OBSIDIAN) {
                    obbySlot = i;
                    break;
                }
            }

            if (support.get() && obbySlot != -1 && mc.world.getBlockState(mineTarget.down(1)).isAir()) {
                PlayerUtils.placeBlock(mineTarget.down(1), obbySlot, Hand.MAIN_HAND);
            } else if (support.get() && obbySlot == -1) if (chatInfo.get()) ChatUtils.moduleWarning(this, "No obsidian found for support, mining anyway.");

            mc.player.inventory.selectedSlot = pickSlot;

            if (rotate.get()) RotationUtils.packetRotate(mineTarget);
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, mineTarget, Direction.UP));
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, mineTarget, Direction.UP));
        }
        toggle();
    }

    @Override
    public String getInfoString() {
        if (target != null && target instanceof PlayerEntity) return target.getEntityName();
        if (target != null) return target.getType().getName().getString();
        return null;
    }
}
