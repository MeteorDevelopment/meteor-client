/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class BetterLocator extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgData = settings.createGroup("Display Data");

    public final Setting<Boolean> displayHeads = sgGeneral.add(new BoolSetting.Builder()
        .name("display-heads")
        .description("Displays player heads instead of dots on the locator bar.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> alwaysShowExperience = sgGeneral.add(new BoolSetting.Builder()
        .name("always-show-experience")
        .description("Always shows the experience bar.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> showDirections = sgGeneral.add(new BoolSetting.Builder()
        .name("show-directions")
        .description("Shows cardinal directions (N, S, E, W) on the locator bar.")
        .defaultValue(true)
        .build()
    );


    public final Setting<Boolean> displayData = sgData.add(new BoolSetting.Builder()
        .name("display-player-data")
        .description("Displays information about the player.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> displayName = sgData.add(new BoolSetting.Builder()
        .name("display-name")
        .description("Displays the player's name.")
        .defaultValue(true)
        .visible(displayData::get)
        .build()
    );

    public final Setting<Boolean> displayCoords = sgData.add(new BoolSetting.Builder()
        .name("display-distance")
        .description("Displays the player's distance.")
        .defaultValue(true)
        .visible(displayData::get)
        .build()
    );



    public final Setting<Boolean> respectColor = sgData.add(new BoolSetting.Builder()
        .name("respect-color")
        .description("Respects the player's color.")
        .defaultValue(false)
        .visible(displayData::get)
        .build()
    );

    public final Setting<Boolean> displayOnlyOnTab = sgData.add(new BoolSetting.Builder()
        .name("display-only-on-tab")
        .description("Only displays data when holding the player list key (Tab).")
        .defaultValue(true)
        .visible(displayData::get)
        .build()
    );
    public BetterLocator() {
        super(Categories.Render, "better-locator", "Modifies the Locator HUD.");
    }
}
