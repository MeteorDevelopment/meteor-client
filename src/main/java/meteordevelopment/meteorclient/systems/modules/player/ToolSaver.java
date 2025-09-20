/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.entity.player.*;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PumpkinBlock;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;

import java.util.List;
import java.util.Objects;

public class ToolSaver extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> durability = sgGeneral.add(new IntSetting.Builder()
        .name("percentage")
        .description("The durability percentage to stop using a tool at")
        .defaultValue(10)
        .range(1, 100)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<Boolean> onlyMending = sgGeneral.add(new BoolSetting.Builder()
        .name("only-mending")
        .description("Only avoid breaking tools which have mending")
        .build()
    );

    private final Setting<Boolean> onlyUnique = sgGeneral.add(new BoolSetting.Builder()
        .name("only-last-tool")
        .description("Only avoid breaking the last tool of a type unless the other tools are all worse")
        .build()
    );

    private final Setting<Boolean> allowAttack = sgGeneral.add(new BoolSetting.Builder()
        .name("allow-manual-attack")
        .description("Never prevent manual attacks")
        .build()
    );

    private final Setting<ListMode> listMode = sgGeneral.add(new EnumSetting.Builder<ListMode>()
        .name("list-mode")
        .description("Selection mode")
        .defaultValue(ListMode.Blacklist)
        .build()
    );

    private final Setting<List<Item>> whitelist = sgGeneral.add(new ItemListSetting.Builder()
        .name("whitelist")
        .description("Do not allow breaking these tools")
        .visible(() -> listMode.get() == ListMode.Whitelist)
        .filter(ToolSaver::isTool)
        .build()
    );

    private final Setting<List<Item>> blacklist = sgGeneral.add(new ItemListSetting.Builder()
        .name("blacklist")
        .description("Allow breaking these tools")
        .visible(() -> listMode.get() == ListMode.Blacklist)
        .filter(ToolSaver::isTool)
        .build()
    );

    public ToolSaver() {
        super(Categories.Player, "tool-saver", "Prevents breaking tools");
    }

    @EventHandler(priority = EventPriority.HIGH - 1)
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        if (!_canUse(mc.player.getMainHandStack()) && mc.world.getBlockState(event.blockPos).getHardness(mc.world, event.blockPos) > 0) {
            mc.options.attackKey.setPressed(false);
            event.cancel();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onInteractBlock(InteractBlockEvent event) {
        ItemStack s = mc.player.getStackInHand(event.hand);
        BlockState bs = mc.world.getBlockState(event.result.getBlockPos());

        switch (toolType(s)) {
        case AXE:
            if (!bs.isIn(BlockTags.LOGS) && !isWaxedCopperBlock(bs.getBlock())) return;
            break;
        case SHEAR:
            if (!(bs.getBlock() instanceof PumpkinBlock)) return;
            break;
        case FLINT_AND_STEEL:
            break;
        default:
            return;
        }

        if (!_canUse(s)) {
            mc.options.useKey.setPressed(false);
            event.cancel();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onInteractEntity(InteractEntityEvent event) {
        ItemStack s = mc.player.getStackInHand(event.hand);

        if (toolType(s) != ToolType.SHEAR) return;
        if (!(event.entity instanceof SheepEntity || event.entity instanceof SnowGolemEntity)) return;

        if (!_canUse(s)) {
            mc.options.useKey.setPressed(false);
            event.cancel();
        }
    }

    private static boolean isWaxedCopperBlock(Block b) {
        // there is no block tag or other apparent way to find waxed blocks...
        return b == Blocks.WAXED_WEATHERED_CHISELED_COPPER
            || b == Blocks.WAXED_EXPOSED_CHISELED_COPPER
            || b == Blocks.WAXED_CHISELED_COPPER
            || b == Blocks.WAXED_COPPER_BLOCK
            || b == Blocks.WAXED_WEATHERED_COPPER
            || b == Blocks.WAXED_EXPOSED_COPPER
            || b == Blocks.WAXED_OXIDIZED_COPPER
            || b == Blocks.WAXED_OXIDIZED_CUT_COPPER
            || b == Blocks.WAXED_WEATHERED_CUT_COPPER
            || b == Blocks.WAXED_EXPOSED_CUT_COPPER
            || b == Blocks.WAXED_CUT_COPPER
            || b == Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS
            || b == Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS
            || b == Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS
            || b == Blocks.WAXED_CUT_COPPER_STAIRS
            || b == Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB
            || b == Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB
            || b == Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB
            || b == Blocks.WAXED_CUT_COPPER_SLAB
            || b == Blocks.WAXED_COPPER_DOOR
            || b == Blocks.WAXED_EXPOSED_COPPER_DOOR
            || b == Blocks.WAXED_OXIDIZED_COPPER_DOOR
            || b == Blocks.WAXED_WEATHERED_COPPER_DOOR
            || b == Blocks.WAXED_COPPER_TRAPDOOR
            || b == Blocks.WAXED_EXPOSED_COPPER_TRAPDOOR
            || b == Blocks.WAXED_OXIDIZED_COPPER_TRAPDOOR
            || b == Blocks.WAXED_WEATHERED_COPPER_TRAPDOOR
            || b == Blocks.WAXED_COPPER_GRATE
            || b == Blocks.WAXED_EXPOSED_COPPER_GRATE
            || b == Blocks.WAXED_WEATHERED_COPPER_GRATE
            || b == Blocks.WAXED_OXIDIZED_COPPER_GRATE
            || b == Blocks.WAXED_COPPER_BULB
            || b == Blocks.WAXED_EXPOSED_COPPER_BULB
            || b == Blocks.WAXED_WEATHERED_COPPER_BULB
            || b == Blocks.WAXED_OXIDIZED_COPPER_BULB;
    }


    @EventHandler(priority = EventPriority.HIGH)
    private void onAttackEntity(AttackEntityEvent event) {
        if (!allowAttack.get() && !_canUse(mc.player.getMainHandStack())) event.cancel();
    }

    private static ToolType toolType(ItemStack is) {
        if (is.isIn(ItemTags.AXES)) return ToolType.AXE;
        if (is.isIn(ItemTags.HOES)) return ToolType.HOE;
        if (is.isIn(ItemTags.PICKAXES)) return ToolType.PICKAXE;
        if (is.isIn(ItemTags.SHOVELS)) return ToolType.SHOVEL;
        if (is.isIn(ItemTags.SWORDS)) return ToolType.SWORD;
        if (is.getItem() instanceof ShearsItem) return ToolType.SHEAR;
        if (is.getItem() instanceof MaceItem) return ToolType.MACE;
        if (is.getItem() instanceof FlintAndSteelItem) return ToolType.FLINT_AND_STEEL;
        return ToolType.NONE;
    }

    public static boolean isTool(Item item) {
        return isTool(item.getDefaultStack());
    }

    public static boolean isTool(ItemStack is) {
        return toolType(is) != ToolType.NONE;
    }

    private boolean isWorse(ItemStack a, ItemStack b) {
        // lower material tier (no-one cares about gold)
        if (a.getMaxDamage() < b.getMaxDamage()) return true;

        // non-mending tools can always be broken if only-mending is set
        if (onlyMending.get() && Utils.getEnchantmentLevel(a, Enchantments.MENDING) < Utils.getEnchantmentLevel(b, Enchantments.MENDING))
            return true;

        // don't break the more enchanted tool
        if (Utils.getEnchantmentLevel(a, Enchantments.FORTUNE) > 0 && Utils.getEnchantmentLevel(a, Enchantments.FORTUNE) < Utils.getEnchantmentLevel(b, Enchantments.FORTUNE))
            return true;
        return Utils.getEnchantmentLevel(a, Enchantments.EFFICIENCY) < Utils.getEnchantmentLevel(b, Enchantments.EFFICIENCY);
    }

    private boolean canBreak(ItemStack is) {
        if (!isTool(is)) return true;
        if (onlyMending.get() && Utils.getEnchantmentLevel(is, Enchantments.MENDING) == 0) return true;
        if (!onlyUnique.get()) return false;

        ToolType tt = toolType(is);

        boolean silkTouch = Utils.getEnchantmentLevel(is, Enchantments.SILK_TOUCH) > 0;

        int counter = 0;

        for (int i = 0; i < 40; i++) {
            ItemStack isi = mc.player.getInventory().getStack(i);

            if (tt != toolType(isi)) continue;
            if (silkTouch != Utils.getEnchantmentLevel(isi, Enchantments.SILK_TOUCH) > 0) continue;

            if (!isWorse(isi, is)) counter++;
            if (counter > 1) return true;
        }

        return false;
    }

    private boolean isBroken(ItemStack tool) {
        return tool.getMaxDamage() - tool.getDamage() < tool.getMaxDamage() * durability.get() / 100;
    }

    private boolean isIgnored(ItemStack tool) {
        if (listMode.get() == ListMode.Whitelist) return !whitelist.get().contains(tool.getItem());
        return blacklist.get().contains(tool.getItem());
    }

    private boolean _canUse(ItemStack tool) {
        return !isActive() || isIgnored(tool) || !isBroken(tool) || canBreak(tool);
    }

    public static boolean canUse(ItemStack tool) {
        return Modules.get().get(ToolSaver.class)._canUse(tool);
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }

    public enum ToolType {
        NONE,
        AXE,
        HOE,
        PICKAXE,
        SHOVEL,
        SWORD,
        MACE,
        SHEAR,
        FLINT_AND_STEEL,
    }
}
