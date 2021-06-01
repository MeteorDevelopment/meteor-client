/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.movement;

import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;

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
            .description("Whether or not cobwebs will not slow you down.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> soulSand = sgGeneral.add(new BoolSetting.Builder()
            .name("soul-sand")
            .description("Whether or not Soul Sand will not slow you down.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> slimeBlock = sgGeneral.add(new BoolSetting.Builder()
            .name("slime-block")
            .description("Whether or not slime blocks will not slow you down.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> airStrict = sgGeneral.add(new BoolSetting.Builder()
            .name("air-strict")
            .description("Will attempt to bypass anti-cheats like 2b2t's. Only works while in air.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> sneaking = sgGeneral.add(new BoolSetting.Builder()
            .name("sneaking")
            .description("Whether or not sneaking will not slow you down.")
            .defaultValue(false)
            .build()
    );

    public NoSlow() {
        super(Categories.Movement, "no-slow", "Allows you to move normally when using objects that will slow you.");
    }

    public boolean airStrict() {
        return isActive() && airStrict.get() && mc.player.isUsingItem() && !mc.player.isOnGround();
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

    public boolean sneaking() {
        return isActive() && sneaking.get();
    }
}