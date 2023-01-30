/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.baritone.BaritoneInfinityMiner;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.List;

public class InfinityMiner extends Module {
    public static InfinityMiner INSTANCE;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWhenFull = settings.createGroup("When Full");

    // General

    public final Setting<List<Block>> targetBlocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("target-blocks")
        .description("The target blocks to mine.")
        .defaultValue(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE)
        .filter(this::filterBlocks)
        .build()
    );

    public final Setting<List<Item>> targetItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("target-items")
        .description("The target items to collect.")
        .defaultValue(Items.DIAMOND)
        .build()
    );

    public final Setting<List<Block>> repairBlocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("repair-blocks")
        .description("The repair blocks to mine.")
        .defaultValue(Blocks.COAL_ORE, Blocks.REDSTONE_ORE, Blocks.NETHER_QUARTZ_ORE)
        .filter(this::filterBlocks)
        .build()
    );

    public final Setting<Double> startRepairing = sgGeneral.add(new DoubleSetting.Builder()
        .name("repair-threshold")
        .description("The durability percentage at which to start repairing.")
        .defaultValue(20)
        .range(1, 99)
        .sliderRange(1, 99)
        .build()
    );

    public final Setting<Double> startMining = sgGeneral.add(new DoubleSetting.Builder()
        .name("mine-threshold")
        .description("The durability percentage at which to start mining.")
        .defaultValue(70)
        .range(1, 99)
        .sliderRange(1, 99)
        .build()
    );

    // When Full

    public final Setting<Boolean> walkHome = sgWhenFull.add(new BoolSetting.Builder()
        .name("walk-home")
        .description("Will walk 'home' when your inventory is full.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> logOut = sgWhenFull.add(new BoolSetting.Builder()
        .name("log-out")
        .description("Logs out when your inventory is full. Will walk home FIRST if walk home is enabled.")
        .defaultValue(false)
        .build()
    );

    public InfinityMiner() {
        super(Categories.World, "infinity-miner", "Allows you to essentially mine forever by mining repair blocks when the durability gets low. Needs a mending pickaxe.");
        INSTANCE = this;
    }

//    BaritoneInfinityMiner miner = new BaritoneInfinityMiner(this);

    @Override
    public void onActivate() {
        try {
            Class.forName("baritone.api.BaritoneAPI");
            BaritoneInfinityMiner.activate();
        } catch (ClassNotFoundException e) {
            error("Baritone not found. Please install Baritone.");
            toggle();
        }
    }

    @Override
    public void onDeactivate() {
        try {
            Class.forName("baritone.api.BaritoneAPI");
            BaritoneInfinityMiner.deactivate();
        } catch (ClassNotFoundException e) {
            error("Baritone not found. Please install Baritone.");
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        BaritoneInfinityMiner.onTick();
    }

    public boolean filterBlocks(Block block) {
        return block != Blocks.AIR && block.getDefaultState().getHardness(mc.world, null) != -1 && !(block instanceof FluidBlock);
    }
}
