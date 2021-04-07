/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.combat;

import minegame159.meteorclient.mixininterface.IClientPlayerInteractionManager;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.player.CityUtils;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.player.Rotations;
import minegame159.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class AutoCity extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The maximum range a city-able block will be found.")
            .defaultValue(5)
            .min(0)
            .sliderMax(20)
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

    private final Setting<Boolean> crystal = sgGeneral.add(new BoolSetting.Builder()
            .name("crystal")
            .description("Places a crystal above the city block.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates you towards the city block.")
            .defaultValue(true)
            .build()
    );

    public AutoCity() {
        super(Categories.Combat, "auto-city", "Automatically cities a target by mining the nearest obsidian next to them.");
    }

    private PlayerEntity target;

    @Override
    public void onActivate() {
        target = CityUtils.getPlayerTarget(range.get());
        BlockPos mineTarget = CityUtils.getTargetBlock(target);

        if (target == null || mineTarget == null) {
            if (chatInfo.get()) ChatUtils.moduleError(this, "No target block found... disabling.");
        } else {
            if (chatInfo.get()) ChatUtils.moduleInfo(this, "Attempting to city " + target.getGameProfile().getName());

            if (MathHelper.sqrt(mc.player.squaredDistanceTo(mineTarget.getX(), mineTarget.getY(), mineTarget.getZ())) > mc.interactionManager.getReachDistance()) {
                if (chatInfo.get()) ChatUtils.moduleError(this, "Target block out of reach... disabling.");
                toggle();
                return;
            }

            int slot = InvUtils.findItemInHotbar(Items.NETHERITE_PICKAXE);
            if (slot == -1) slot = InvUtils.findItemInHotbar(Items.DIAMOND_PICKAXE);
            if (mc.player.abilities.creativeMode) slot = mc.player.inventory.selectedSlot;

            if (slot == -1) {
                if (chatInfo.get()) ChatUtils.moduleError(this, "No pick found... disabling.");
                toggle();
                return;
            }


            if (support.get()) {
                int obbySlot = InvUtils.findItemInHotbar(Items.OBSIDIAN);
                BlockPos blockPos = mineTarget.down(1);

                if (!BlockUtils.canPlace(blockPos)
                        && mc.world.getBlockState(blockPos).getBlock() != Blocks.OBSIDIAN
                        && mc.world.getBlockState(blockPos).getBlock() != Blocks.BEDROCK
                        && chatInfo.get()) {
                    ChatUtils.moduleWarning(this, "Couldn't place support block, mining anyway.");
                }
                else {
                    if (obbySlot == -1) {
                        if (chatInfo.get()) ChatUtils.moduleWarning(this, "No obsidian found for support, mining anyway.");
                    }
                    else {
                        BlockUtils.place(blockPos, Hand.MAIN_HAND, obbySlot, rotate.get(), 0, true);
                    }
                }
            }

            mc.player.inventory.selectedSlot = slot;

            if (rotate.get()) Rotations.rotate(Rotations.getYaw(mineTarget), Rotations.getPitch(mineTarget), () -> mine(mineTarget));
            else mine(mineTarget);

            if (crystal.get()) {
                if (!BlockUtils.canPlace(mineTarget, true)) return;
                Hand hand = InvUtils.getHand(Items.END_CRYSTAL);

                if (hand == Hand.MAIN_HAND) {
                    int crystalSlot = InvUtils.findItemInHotbar(Items.END_CRYSTAL);
                    int preSlot = mc.player.inventory.selectedSlot;
                    mc.player.inventory.selectedSlot = crystalSlot;
                    ((IClientPlayerInteractionManager) mc.interactionManager).syncSelectedSlot2();
                    mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, new BlockHitResult(mc.player.getPos(), Direction.UP, mineTarget, false)));
                    mc.player.inventory.selectedSlot = preSlot;
                } else if (hand == Hand.OFF_HAND) {
                    mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, new BlockHitResult(mc.player.getPos(), Direction.UP, mineTarget, false)));
                }
            }
        }

        this.toggle();
    }

    private void mine(BlockPos blockPos) {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
    }

    @Override
    public String getInfoString() {
        if (target != null) return target.getEntityName();
        return null;
    }
}
