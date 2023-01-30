/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.baritone;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.process.ICustomGoalProcess;
import baritone.api.process.IMineProcess;
import meteordevelopment.meteorclient.systems.modules.world.InfinityMiner;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.function.Predicate;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BaritoneInfinityMiner {
    private static final InfinityMiner module = InfinityMiner.INSTANCE;
    private static final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
    private static final Settings baritoneSettings = BaritoneAPI.getSettings();
    private static final BlockPos.Mutable homePos = new BlockPos.Mutable();

    private static boolean prevMineScanDroppedItems;
    private static boolean repairing;


    public static void activate() {
        prevMineScanDroppedItems = baritoneSettings.mineScanDroppedItems.value;
        baritoneSettings.mineScanDroppedItems.value = true;
        homePos.set(mc.player.getBlockPos());
        repairing = false;
    }

    public static void deactivate() {
        baritone.getPathingBehavior().cancelEverything();
        baritoneSettings.mineScanDroppedItems.value = prevMineScanDroppedItems;
    }

    public static void onTick() {
        if (isFull()) {
            if (module.walkHome.get()) {
                if (isBaritoneNotWalking()) {
                    module.info("Walking home.");
                    baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(homePos));
                }
                else if (mc.player.getBlockPos().equals(homePos) && module.logOut.get()) logOut();
            }
            else if (module.logOut.get()) logOut();
            else {
                module.info("Inventory full, stopping process.");
                module.toggle();
            }

            return;
        }

        if (!findPickaxe()) {
            module.error("Could not find a usable mending pickaxe.");
            module.toggle();
            return;
        }

        if (!checkThresholds()) {
            module.error("Start mining value can't be lower than start repairing value.");
            module.toggle();
            return;
        }

        if (repairing) {
            if (!needsRepair()) {
                module.warning("Finished repairing, going back to mining.");
                repairing = false;
                mineTargetBlocks();
                return;
            }

            if (isBaritoneNotMining()) mineRepairBlocks();
        }
        else {
            if (needsRepair()) {
                module.warning("Pickaxe needs repair, beginning repair process");
                repairing = true;
                mineRepairBlocks();
                return;
            }

            if (isBaritoneNotMining()) mineTargetBlocks();
        }
    }

    private static boolean needsRepair() {
        ItemStack itemStack = mc.player.getMainHandStack();
        double toolPercentage = ((itemStack.getMaxDamage() - itemStack.getDamage()) * 100f) / (float) itemStack.getMaxDamage();
        return !(toolPercentage > module.startMining.get() || (toolPercentage > module.startRepairing.get() && !repairing));
    }

    private static boolean findPickaxe() {
        Predicate<ItemStack> pickaxePredicate = (stack -> stack.getItem() instanceof PickaxeItem
            && Utils.hasEnchantments(stack, Enchantments.MENDING)
            && !Utils.hasEnchantments(stack, Enchantments.SILK_TOUCH));
        FindItemResult bestPick = InvUtils.findInHotbar(pickaxePredicate);

        if (bestPick.isOffhand()) InvUtils.quickMove().fromOffhand().toHotbar(mc.player.getInventory().selectedSlot);
        else if (bestPick.isHotbar()) InvUtils.swap(bestPick.slot(), false);

        return InvUtils.testInMainHand(pickaxePredicate);
    }

    private static boolean checkThresholds() {
        return module.startRepairing.get() < module.startMining.get();
    }

    private static void mineTargetBlocks() {
        Block[] array = new Block[module.targetBlocks.get().size()];

        baritone.getPathingBehavior().cancelEverything();
        baritone.getMineProcess().mine(module.targetBlocks.get().toArray(array));
    }

    private static void mineRepairBlocks() {
        Block[] array = new Block[module.repairBlocks.get().size()];

        baritone.getPathingBehavior().cancelEverything();
        baritone.getMineProcess().mine(module.repairBlocks.get().toArray(array));
    }

    private static void logOut() {
        module.toggle();
        mc.player.networkHandler.sendPacket(new DisconnectS2CPacket(Text.literal("[Infinity Miner] Inventory is full.")));
    }

    private static boolean isBaritoneNotMining() {
        return !(baritone.getPathingControlManager().mostRecentInControl().orElse(null) instanceof IMineProcess);
    }

    private static boolean isBaritoneNotWalking() {
        return !(baritone.getPathingControlManager().mostRecentInControl().orElse(null) instanceof ICustomGoalProcess);
    }



    private static boolean isFull() {
        for (int i = 0; i <= 35; i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);

            for (Item item : module.targetItems.get()) {
                if ((itemStack.getItem() == item && itemStack.getCount() < itemStack.getMaxCount())
                    || itemStack.isEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }
}
