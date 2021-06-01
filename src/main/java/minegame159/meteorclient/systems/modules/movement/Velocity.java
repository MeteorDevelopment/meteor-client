/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.movement;

import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;

public class Velocity extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> entities = sgGeneral.add(new BoolSetting.Builder()
            .name("entities")
            .description("Modifies the amount of knockback you take from entities and attacks.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Double> entitiesHorizontal = sgGeneral.add(new DoubleSetting.Builder()
            .name("entities-horizontal")
            .description("How much velocity you will take from entities horizontally.")
            .defaultValue(0)
            .sliderMin(0).sliderMax(1)
            .visible(entities::get)
            .build()
    );

    public final Setting<Double> entitiesVertical = sgGeneral.add(new DoubleSetting.Builder()
            .name("entities-vertical")
            .description("How much velocity you will take from entities vertically.")
            .defaultValue(0)
            .sliderMin(0).sliderMax(1)
            .visible(entities::get)
            .build()
    );

    public final Setting<Boolean> explosions = sgGeneral.add(new BoolSetting.Builder()
            .name("explosions")
            .description("Modifies your knockback from explosions.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Double> explosionsHorizontal = sgGeneral.add(new DoubleSetting.Builder()
            .name("explosions-horizontal")
            .description("How much velocity you will take from explosions horizontally.")
            .defaultValue(0)
            .sliderMin(0).sliderMax(1)
            .visible(explosions::get)
            .build()
    );

    public final Setting<Double> explosionsVertical = sgGeneral.add(new DoubleSetting.Builder()
            .name("explosions-vertical")
            .description("How much velocity you will take from explosions vertically.")
            .defaultValue(0)
            .sliderMin(0).sliderMax(1)
            .visible(explosions::get)
            .build()
    );

    public final Setting<Boolean> liquids = sgGeneral.add(new BoolSetting.Builder()
            .name("liquids")
            .description("Modifies the amount you are pushed by flowing liquids.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Double> liquidsHorizontal = sgGeneral.add(new DoubleSetting.Builder()
            .name("liquids-horizontal")
            .description("How much velocity you will take from liquids horizontally.")
            .defaultValue(0)
            .sliderMin(0).sliderMax(1)
            .visible(liquids::get)
            .build()
    );

    public final Setting<Double> liquidsVertical = sgGeneral.add(new DoubleSetting.Builder()
            .name("liquids-vertical")
            .description("How much velocity you will take from liquids vertically.")
            .defaultValue(0)
            .sliderMin(0).sliderMax(1)
            .visible(liquids::get)
            .build()
    );

    public final Setting<Boolean> blocks = sgGeneral.add(new BoolSetting.Builder()
            .name("blocks")
            .description("Prevents you from being pushed out of blocks.")
            .defaultValue(true)
            .build()
    );

    public Velocity() {
        super(Categories.Movement, "velocity", "Prevents you from being moved by external forces.");
    }

    public double getHorizontal(Setting<Double> setting) {
        return isActive() ? setting.get() : 1;
    }
    public double getVertical(Setting<Double> setting) {
        return isActive() ? setting.get() : 1;
    }

}