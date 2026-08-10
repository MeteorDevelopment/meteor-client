/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.baritone;

import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** One-shot command: press its keybind to have Baritone mine the configured blocks. */
public class Mine extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Blocks to mine.")
        .build()
    );

    public Mine() {
        super(Categories.Baritone, "mine", "Has Baritone mine the configured blocks.");
    }

    @Override
    public void onActivate() {
        if (BaritoneUtil.check(this)) {
            if (blocks.get().isEmpty()) warning("No blocks selected.");
            else PathManagers.get().mine(blocks.get().toArray(new Block[0]));
        }

        toggle();
    }
}
