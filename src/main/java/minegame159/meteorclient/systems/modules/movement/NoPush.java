/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.movement;

import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;

public class NoPush extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> entities = sgGeneral.add(new BoolSetting.Builder()
            .name("Entities")
            .description("Prevents you from being pushed by entities.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Boolean> blocks = sgGeneral.add(new BoolSetting.Builder()
            .name("Blocks")
            .description("Prevents you from being pushed out of blocks.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Boolean> liquids = sgGeneral.add(new BoolSetting.Builder()
            .name("Liquids")
            .description("Prevents you from being pushed by liquids.")
            .defaultValue(true)
            .build()
    );


    public NoPush() {
        super(Categories.Movement, "No Push", "Prevents you from being pushed.");
    }
}
