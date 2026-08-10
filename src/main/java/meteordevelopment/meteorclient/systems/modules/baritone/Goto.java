/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.baritone;

import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.BlockPosSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.core.BlockPos;

/** One-shot command: press its keybind to send Baritone to the configured target. */
public class Goto extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<BlockPos> target = sgGeneral.add(new BlockPosSetting.Builder()
        .name("target")
        .description("Destination to path to.")
        .build()
    );

    private final Setting<Boolean> ignoreY = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-y")
        .description("Reach the X and Z of the target, ignoring its Y.")
        .defaultValue(false)
        .build()
    );

    public Goto() {
        super(Categories.Baritone, "goto", "Sends Baritone to a fixed coordinate.");
    }

    @Override
    public void onActivate() {
        if (BaritoneUtil.check(this)) PathManagers.get().moveTo(target.get(), ignoreY.get());

        toggle();
    }
}
