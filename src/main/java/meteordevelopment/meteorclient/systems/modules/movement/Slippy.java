/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.block.Block;

import java.util.List;

public class Slippy extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Double> friction = sgGeneral.add(new DoubleSetting.Builder()
        .name("friction")
        .description("The base friction level.")
        .range(0.01, 1.10)
        .sliderRange(0.01, 1.10)
        .defaultValue(1)
        .build()
    );

    public final Setting<ListMode> listMode = sgGeneral.add(new EnumSetting.Builder<ListMode>()
        .name("list-mode")
        .description("The mode to select blocks.")
        .defaultValue(ListMode.Blacklist)
        .build()
    );

    public final Setting<List<Block>> ignoredBlocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("ignored-blocks")
        .description("Decide which blocks not to slip on")
        .visible(() -> listMode.get() == ListMode.Blacklist)
        .build()
    );

    public final Setting<List<Block>> allowedBlocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("allowed-blocks")
        .description("Decide which blocks to slip on")
        .visible(() -> listMode.get() == ListMode.Whitelist)
        .build()
    );

    public Slippy() {
        super(Categories.Movement, "slippy", "Changes the base friction level of blocks.");
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }
}
