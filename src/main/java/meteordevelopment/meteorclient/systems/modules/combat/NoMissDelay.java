/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class NoMissDelay extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public NoMissDelay() {
        super(Categories.Combat, "no-miss-delay", "Cancels attacks that would miss");
    }

    private final Setting<Boolean> cancelblockattacks = sgGeneral.add(new BoolSetting.Builder()
        .name("block-attacks")
        .description("Cancels attacks that hit a block.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> onlywithsword = sgGeneral.add(new BoolSetting.Builder()
        .name("only-weapons")
        .description("Cancels attacks only when holding a weapon")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
        .name("swing")
        .description("Wheter to swing the hand when cancelling attacks")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> cancelnoncrits = sgGeneral.add(new BoolSetting.Builder()
        .name("cancel-non-crits")
        .description("Whether to cancel attacks that are not critical hits")
        .defaultValue(false)
        .build()
    );

    public boolean shouldSwing() {
        return swing.get();
    }

    public boolean getCancelblockattacks() {
        return cancelblockattacks.get();
    }

    public boolean getOnlywithsword() {
        return onlywithsword.get();
    }

    public boolean getCancelnoncrits() {
        return cancelnoncrits.get();
    }
}
