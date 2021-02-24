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

public class RainbowColors {
    private static final List<Setting<SettingColor>> colorSettings = new ArrayList<>();
    private static final List<SettingColor> colors = new ArrayList<>();

    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(RainbowColors.class);
    }

    public static void addSetting(Setting<SettingColor> setting) {
        colorSettings.add(setting);
    }
    public static void removeSetting(Setting<SettingColor> setting) {
        colorSettings.remove(setting);
    }

    public static void add(SettingColor color) {
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

        for (Waypoint waypoint : Waypoints.get()) {
            waypoint.color.update();
        }
    }
}
