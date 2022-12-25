/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class EntityControl extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> maxJump = sgGeneral.add(new BoolSetting.Builder()
        .name("max-jump")
        .description("Sets jump power to maximum.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> saddleSpoof = sgGeneral.add(new BoolSetting.Builder()
        .name("saddle-spoof")
        .description("Lets you control rideable entities without a saddle.")
        .defaultValue(false)
        .build()
    );

    public EntityControl() {
        super(Categories.Movement, "entity-control", "Lets you control rideable entities without a saddle.");
    }

    public boolean saddleSpoof() {
        return isActive() && saddleSpoof.get();
    }

    public boolean maxJump() {
        return isActive() && maxJump.get();
    }
}
