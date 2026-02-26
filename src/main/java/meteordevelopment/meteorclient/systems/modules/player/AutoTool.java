/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;


import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import meteordevelopment.meteorclient.systems.modules.world.InfinityMiner;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;

import java.util.List;
import java.util.function.Predicate;

public class AutoTool extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");

    // General

    private final Setting<EnchantPreference> prefer = sgGeneral.add(new EnumSetting.Builder<EnchantPreference>()
        .name("prefer")
        .description("Either to prefer Silk Touch, Fortune, or none.")
        .defaultValue(EnchantPreference.Fortune)
        .build()
    );

    private final Setting<Boolean> silkTouchForEnderChest = sgGeneral.add(new BoolSetting.Builder()
        .name("silk-touch-for-ender-chest")
        .description("Mines Ender Chests only with the Silk Touch enchantment.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> silkTouchForGlass = sgGeneral.add(new BoolSetting.Builder()
        .name("silk-touch-for-glass")
        .description("Prefer to mine glass with silk touch")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> fortuneForOresCrops = sgGeneral.add(new BoolSetting.Builder()
        .name("fortune-for-ores-and-crops")
        .description("Mines Ores and crops only with the Fortune enchantment.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> switchBack = sgGeneral.add(new BoolSetting.Builder()
        .name("switch-back")
        .description("Switches your hand to whatever was selected when releasing your attack key.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> switchAway = sgGeneral.add(new BoolSetting.Builder()
        .name("switch-away")
        .description("Switch to hand when no correct tool is found")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> invSwap = sgGeneral.add(new BoolSetting.Builder()
        .name("swap-from-inventory")
        .description("Search tools in the entire inventory")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> invSwapSlot = sgGeneral.add(new IntSetting.Builder()
        .name("swap-slot")
        .description("Slot to swap tools from inventory into")
        .defaultValue(8)
        .range(0, 8)
        .noSlider()
        .visible(invSwap::get)
        .build()
    );

    private final Setting<Boolean> invSwapReturn = sgGeneral.add(new BoolSetting.Builder()
        .name("return-swapped-tool-back")
        .description("Swap the tool back into the inventory")
        .defaultValue(false)
        .visible(invSwap::get)
        .build()
    );

    private final Setting<Integer> switchDelay = sgGeneral.add((new IntSetting.Builder()
        .name("switch-delay")
        .description("Delay in ticks before switching tools.")
        .defaultValue(0)
        .build()
    ));

    // Whitelist and blacklist

    private final Setting<ListMode> listMode = sgWhitelist.add(new EnumSetting.Builder<ListMode>()
        .name("list-mode")
        .description("Selection mode.")
        .defaultValue(ListMode.Blacklist)
        .build()
    );

    private final Setting<List<Item>> whitelist = sgWhitelist.add(new ItemListSetting.Builder()
        .name("whitelist")
        .description("The tools you want to use.")
        .visible(() -> listMode.get() == ListMode.Whitelist)
        .filter(AutoTool::isTool)
        .build()
    );

    private final Setting<List<Item>> blacklist = sgWhitelist.add(new ItemListSetting.Builder()
        .name("blacklist")
        .description("The tools you don't want to use.")
        .visible(() -> listMode.get() == ListMode.Blacklist)
        .filter(AutoTool::isTool)
        .build()
    );

    private boolean wasPressed;
    private boolean shouldSwitch;
    private int ticks;
    private int bestSlot;
    private int toolWasIn;

    public AutoTool() {
        super(Categories.Player, "auto-tool", "Automatically switches to the most effective tool when performing an action.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (Modules.get().isActive(InfinityMiner.class)) return;

        if (invSwapReturn.get() && !mc.options.attackKey.isPressed() && wasPressed && toolWasIn != -1) {
            InvUtils.quickSwap().fromId(invSwapSlot.get()).to(toolWasIn);
            toolWasIn = -1;
            return;
        }

        if (switchBack.get() && !mc.options.attackKey.isPressed() && wasPressed && InvUtils.previousSlot != -1) {
            InvUtils.swapBack();
            wasPressed = false;
            return;
        }

        if (ticks <= 0 && shouldSwitch && bestSlot != -1) {
            InvUtils.swap(bestSlot, switchBack.get());
            shouldSwitch = false;
        } else {
            ticks--;
        }

        wasPressed = mc.options.attackKey.isPressed();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        if (Modules.get().isActive(InfinityMiner.class)) return;
        if (mc.player.isCreative()) return;

        // Get blockState
        BlockState blockState = mc.world.getBlockState(event.blockPos);
        if (!BlockUtils.canBreak(event.blockPos, blockState)) return;

        // Check if we should switch to a better tool
        ItemStack currentStack = mc.player.getMainHandStack();

        double bestScore = -1;
        bestSlot = -1;

        int max = invSwap.get() ? SlotUtils.MAIN_END : 9;

        for (int i = 0; i < max; i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);

            if (listMode.get() == ListMode.Whitelist && !whitelist.get().contains(itemStack.getItem())) continue;
            if (listMode.get() == ListMode.Blacklist && blacklist.get().contains(itemStack.getItem())) continue;

            EnchantPreference pref = isGlass(blockState.getBlock()) && silkTouchForGlass.get() ? EnchantPreference.SilkTouch : prefer.get();

            double score = getScore(itemStack, blockState, silkTouchForEnderChest.get(), fortuneForOresCrops.get(), pref, ToolSaver::canUse);
            if (score < 0) continue;

            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        if (bestSlot == -1 && ToolSaver.isTool(mc.player.getMainHandStack()) && switchAway.get()) {
            for (int i = 0; i < 9; i++) {
                if (!ToolSaver.isTool(mc.player.getInventory().getStack(i))) {
                    bestSlot = i;
                    bestScore = 0;
                    break;
                }
            }
        }

        int returnToolTo = toolWasIn;

        if (bestSlot > 8) {
            toolWasIn = bestSlot;
            bestSlot = invSwapSlot.get();
        }

        if ((bestSlot != -1 && (bestScore > getScore(currentStack, blockState, silkTouchForEnderChest.get(), fortuneForOresCrops.get(), prefer.get(), itemStack -> ToolSaver.canUse(itemStack) || ToolSaver.canUse(currentStack) || !isTool(currentStack))))) {
            ticks = switchDelay.get();

            if (invSwapReturn.get() && returnToolTo > 8) InvUtils.quickSwap().fromId(invSwapSlot.get()).to(returnToolTo);

            if (ticks == 0) InvUtils.swap(bestSlot, true);
            else shouldSwitch = true;

            if (toolWasIn > 8) {
                if (bestSlot == invSwapSlot.get()) InvUtils.quickSwap().fromId(invSwapSlot.get()).to(toolWasIn);
                else toolWasIn = -1;
            }
        }

    }

    public static double getScore(ItemStack itemStack, BlockState state, boolean silkTouchEnderChest, boolean fortuneOre, EnchantPreference enchantPreference, Predicate<ItemStack> good) {
        if (!good.test(itemStack) || !isTool(itemStack)) return -1;

        if (!itemStack.isSuitableFor(state) &&
            !(itemStack.isIn(ItemTags.SWORDS) && (state.getBlock() instanceof BambooBlock || state.getBlock() instanceof BambooShootBlock)) &&
            !(itemStack.getItem() instanceof ShearsItem && state.getBlock() instanceof LeavesBlock || state.isIn(BlockTags.WOOL)) &&
            !(isGlass(state.getBlock()) && enchantPreference == EnchantPreference.SilkTouch && Utils.getEnchantmentLevel(itemStack, Enchantments.SILK_TOUCH) > 0))
            return -1;

        if (silkTouchEnderChest
            && state.getBlock() == Blocks.ENDER_CHEST
            && !Utils.hasEnchantments(itemStack, Enchantments.SILK_TOUCH)) {
            return -1;
        }

        if (fortuneOre
            && isFortunable(state.getBlock())
            && !Utils.hasEnchantments(itemStack, Enchantments.FORTUNE)) {
            return -1;
        }

        double score = 0;

        score += itemStack.getMiningSpeedMultiplier(state) * 1000;
        score += Utils.getEnchantmentLevel(itemStack, Enchantments.UNBREAKING);
        score += Utils.getEnchantmentLevel(itemStack, Enchantments.EFFICIENCY);
        score += Utils.getEnchantmentLevel(itemStack, Enchantments.MENDING);

        if (enchantPreference == EnchantPreference.Fortune) score += Utils.getEnchantmentLevel(itemStack, Enchantments.FORTUNE);
        if (enchantPreference == EnchantPreference.SilkTouch) score += Utils.getEnchantmentLevel(itemStack, Enchantments.SILK_TOUCH);

        if (itemStack.isIn(ItemTags.SWORDS) && (state.getBlock() instanceof BambooBlock || state.getBlock() instanceof BambooShootBlock))
            score += 9000 + (itemStack.get(DataComponentTypes.TOOL).getSpeed(state) * 1000);

        return score;
    }

    private static boolean isGlass(Block b) {
        return b == Blocks.GLASS || b == Blocks.GLASS_PANE || b instanceof StainedGlassBlock || b instanceof StainedGlassPaneBlock || b instanceof TintedGlassBlock;
    }

    public static boolean isTool(Item item) {
        return isTool(item.getDefaultStack());
    }

    public static boolean isTool(ItemStack itemStack) {
        return itemStack.isIn(ItemTags.AXES) || itemStack.isIn(ItemTags.HOES) || itemStack.isIn(ItemTags.PICKAXES) || itemStack.isIn(ItemTags.SHOVELS) || itemStack.getItem() instanceof ShearsItem;
    }

    private static boolean isFortunable(Block block) {
        if (block == Blocks.ANCIENT_DEBRIS) return false;
        return Xray.ORES.contains(block) || block instanceof CropBlock;
    }

    public enum EnchantPreference {
        None,
        Fortune,
        SilkTouch
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }
}
