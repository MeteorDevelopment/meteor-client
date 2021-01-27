/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.render.color;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.waypoints.Waypoint;
import minegame159.meteorclient.waypoints.Waypoints;

import java.util.ArrayList;
import java.util.List;

public class RainbowColorManager {
    private static final List<Setting<SettingColor>> colorSettings = new ArrayList<>();
    private static final List<SettingColor> colors = new ArrayList<>();

    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(RainbowColorManager.class);
    }

    public static void addColorSetting(Setting<SettingColor> setting) {
        colorSettings.add(setting);
    }

    public static void addColor(SettingColor color) {
        colors.add(color);
    }

    @EventHandler
    private static void onTick(TickEvent.Post event) {
        for (Setting<SettingColor> setting : colorSettings) {
            if (setting.module == null || setting.module.isActive()) setting.get().update();
        }

        for (SettingColor color : colors) {
            color.update();
        }

        for (Waypoint waypoint : Waypoints.INSTANCE) {
            waypoint.color.update();
        }
    }
}
