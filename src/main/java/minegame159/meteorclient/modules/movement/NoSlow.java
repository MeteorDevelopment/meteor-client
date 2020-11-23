/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.movement;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class NoSlow extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> items = sgGeneral.add(new BoolSetting.Builder()
            .name("items")
            .description("No slow from using items.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> web = sgGeneral.add(new BoolSetting.Builder()
            .name("web")
            .description("No slow from cob webs.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> soulSand = sgGeneral.add(new BoolSetting.Builder()
            .name("soul-sand")
            .description("No slow from soul sand.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> slimeBlock = sgGeneral.add(new BoolSetting.Builder()
            .name("slime-block")
            .description("No slow from slime blocks.")
            .defaultValue(true)
            .build()
    );

    public NoSlow() {
        super(Category.Movement, "no-slow", "Allows you to move normally while eating, using items, etc.");
    }

    public boolean items() {
        return isActive() && items.get();
    }

    public boolean web() {
        return isActive() && web.get();
    }

    public boolean soulSand() {
        return isActive() && soulSand.get();
    }

    public boolean slimeBlock() {
        return isActive() && slimeBlock.get();
    }
}
