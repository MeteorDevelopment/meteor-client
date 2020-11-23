/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class ItemByteSize extends ToggleModule {
    public enum Mode {
        Standard, True
    }

    private final SettingGroup sgUseKbIfBigEnough = settings.createGroup("Use KB if big enough");

    private final Setting<Boolean> useKbIfBigEnoughEnabled = sgUseKbIfBigEnough.add(new BoolSetting.Builder()
            .name("use-kb-if-big-enough-enabled")
            .description("Uses kilobytes instead of bytes if the item is larger than 1 kb.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Mode> mode = sgUseKbIfBigEnough.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Standard 1 kb = 1000 b, True 1 kb = 1024 b.")
            .defaultValue(Mode.True)
            .build()
    );

    public ItemByteSize() {
        super(Category.Misc, "item-byte-size", "Displays item's size in bytes in tooltip.");
    }

    private int getKbSize() {
        return mode.get() == Mode.True ? 1024 : 1000;
    }

    public String bytesToString(int count) {
        if (useKbIfBigEnoughEnabled.get() && count >= getKbSize()) return String.format("%.2f kb", count / (float) getKbSize());
        return String.format("%d bytes", count);
    }
}
