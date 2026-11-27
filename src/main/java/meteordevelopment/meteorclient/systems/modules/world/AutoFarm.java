/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AutoFarm extends Module {
    private static final int MAX_REPLANT_QUEUE = 64;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgReplant = settings.createGroup("Replant");

    private final Setting<List<Item>> crops = sgGeneral.add(new ItemListSetting.Builder()
        .name("crops")
        .description("Crops to harvest.")
        .defaultValue(Items.WHEAT, Items.CARROT, Items.POTATO, Items.BEETROOT, Items.NETHER_WART, Items.COCOA_BEANS, Items.PUMPKIN, Items.MELON_SLICE, Items.SWEET_BERRIES, Items.SUGAR_CANE, Items.CACTUS)
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The range around you to harvest crops in.")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Automatically faces the crops being harvested and replanted.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> replant = sgReplant.add(new BoolSetting.Builder()
        .name("replant")
        .description("Automatically replants harvested crops.")
        .defaultValue(true)
        .build()
    );

    private final Map<BlockPos, Item> toReplant = new LinkedHashMap<>();

    public AutoFarm() {
        super(Categories.World, "auto-farm", "Automatically harvests and replants crops around you.");
    }

    @Override
    public void onActivate() {
        toReplant.clear();
    }

    @Override
    public void onDeactivate() {
        toReplant.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Replant before harvesting so freed farmland is used again as soon as possible
        if (replant.get() && replantOne()) return;

        harvestOne();
    }

    /**
     * Replants the first queued position that can be replanted. One action per tick.
     */
    private boolean replantOne() {
        Iterator<Map.Entry<BlockPos, Item>> it = toReplant.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<BlockPos, Item> entry = it.next();
            BlockPos pos = entry.getKey();

            // The position was refilled by something else, no need to replant
            if (!mc.level.getBlockState(pos).isAir()) {
                it.remove();
                continue;
            }

            FindItemResult findSeed = InvUtils.findInHotbar(itemStack -> itemStack.is(entry.getValue()));
            if (!findSeed.found()) continue;

            if (BlockUtils.place(pos, findSeed, rotate.get(), -100)) {
                it.remove();
                return true;
            }
        }

        return false;
    }

    /**
     * Harvests the first harvestable crop in range. One action per tick.
     */
    private void harvestOne() {
        int r = (int) Math.ceil(range.get());
        BlockPos center = mc.player.blockPosition();

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            BlockState state = mc.level.getBlockState(pos);
            Item item = harvestItem(pos, state);

            if (item == null || !crops.get().contains(item)) continue;

            // Berry bushes are harvested by interacting instead of breaking
            if (state.getBlock() instanceof SweetBerryBushBlock) {
                Vec3 hitPos = Vec3.atCenterOf(pos);
                BlockHitResult blockHitResult = new BlockHitResult(hitPos, BlockUtils.getDirection(pos), pos, false);

                if (rotate.get()) Rotations.rotate(Rotations.getYaw(hitPos), Rotations.getPitch(hitPos), -100, () -> BlockUtils.interact(blockHitResult, InteractionHand.MAIN_HAND, true));
                else BlockUtils.interact(blockHitResult, InteractionHand.MAIN_HAND, true);

                return;
            }

            if (replant.get()) {
                Item seed = seedOf(state);
                if (seed != null) queueReplant(pos, seed);
            }

            if (rotate.get()) Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), -100, () -> BlockUtils.breakBlock(pos, true));
            else BlockUtils.breakBlock(pos, true);

            return;
        }
    }

    /**
     * Returns the item this block drops when harvested, if it is ready to be harvested, otherwise null.
     * <p>
     * Pumpkin and melon blocks are always harvested when present as they only appear once fully grown.
     * Sugar cane and cactus are harvested when there is a block above them so their base is preserved.
     */
    private Item harvestItem(BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state) ? cropOf(state) : null;
        }

        if (state.getBlock() instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE ? Items.NETHER_WART : null;
        }

        if (state.getBlock() instanceof CocoaBlock) {
            return state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE ? Items.COCOA_BEANS : null;
        }

        if (state.getBlock() instanceof SweetBerryBushBlock) {
            // 3 is the max age, harvesting resets it back to 2
            return state.getValue(SweetBerryBushBlock.AGE) >= 3 ? Items.SWEET_BERRIES : null;
        }

        if (state.is(Blocks.PUMPKIN)) return Items.PUMPKIN;
        if (state.is(Blocks.MELON)) return Items.MELON_SLICE;

        if (state.is(Blocks.SUGAR_CANE) && mc.level.getBlockState(pos.above()).is(Blocks.SUGAR_CANE)) return Items.SUGAR_CANE;
        if (state.is(Blocks.CACTUS) && mc.level.getBlockState(pos.above()).is(Blocks.CACTUS)) return Items.CACTUS;

        return null;
    }

    /**
     * Returns the harvested item of a farmland crop block, or null if it is not one.
     */
    private Item cropOf(BlockState state) {
        if (state.is(Blocks.WHEAT)) return Items.WHEAT;
        if (state.is(Blocks.CARROTS)) return Items.CARROT;
        if (state.is(Blocks.POTATOES)) return Items.POTATO;
        if (state.is(Blocks.BEETROOTS)) return Items.BEETROOT;

        return null;
    }

    /**
     * Returns the item needed to replant this crop block, or null if it regrows on its own.
     */
    private Item seedOf(BlockState state) {
        if (state.is(Blocks.WHEAT)) return Items.WHEAT_SEEDS;
        if (state.is(Blocks.CARROTS)) return Items.CARROT;
        if (state.is(Blocks.POTATOES)) return Items.POTATO;
        if (state.is(Blocks.BEETROOTS)) return Items.BEETROOT_SEEDS;
        if (state.is(Blocks.NETHER_WART)) return Items.NETHER_WART;

        return null;
    }

    private void queueReplant(BlockPos pos, Item seed) {
        // Copy the position as it is reused while iterating
        BlockPos immutable = pos.immutable();

        if (!toReplant.containsKey(immutable) && toReplant.size() >= MAX_REPLANT_QUEUE) {
            toReplant.remove(toReplant.keySet().iterator().next());
        }

        toReplant.put(immutable, seed);
    }
}
