/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.movement;

import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class NoSlow extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> items = sgGeneral.add(new BoolSetting.Builder()
            .name("items")
            .description("Whether or not using items will slow you.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> web = sgGeneral.add(new BoolSetting.Builder()
            .name("web")
            .description("Whether or not cobwebs will slow you.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> soulSand = sgGeneral.add(new BoolSetting.Builder()
            .name("soul-sand")
            .description("Whether or not Soul Sand will slow you.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> slimeBlock = sgGeneral.add(new BoolSetting.Builder()
            .name("slime-block")
            .description("Whether or not slime blocks will slow you.")
            .defaultValue(true)
            .build()
    );

    public NoSlow() {
        super(Categories.Movement, "no-slow", "Allows you to move normally when using objects that will slow you.");
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
