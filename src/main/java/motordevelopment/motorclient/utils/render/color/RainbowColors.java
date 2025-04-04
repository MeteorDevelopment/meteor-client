/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.utils.render.color;

import motordevelopment.motorclient.MotorClient;
import motordevelopment.motorclient.events.world.TickEvent;
import motordevelopment.motorclient.gui.GuiThemes;
import motordevelopment.motorclient.gui.WidgetScreen;
import motordevelopment.motorclient.settings.ColorSetting;
import motordevelopment.motorclient.settings.Setting;
import motordevelopment.motorclient.settings.SettingGroup;
import motordevelopment.motorclient.systems.config.Config;
import motordevelopment.motorclient.systems.waypoints.Waypoint;
import motordevelopment.motorclient.systems.waypoints.Waypoints;
import motordevelopment.motorclient.utils.PostInit;
import motordevelopment.motorclient.utils.misc.UnorderedArrayList;
import meteordevelopment.orbit.EventHandler;

import java.util.List;

import static motordevelopment.motorclient.MotorClient.mc;

public class RainbowColors {
    private static final List<Setting<SettingColor>> colorSettings = new UnorderedArrayList<>();
    private static final List<Setting<List<SettingColor>>> colorListSettings = new UnorderedArrayList<>();

    private static final List<SettingColor> colors = new UnorderedArrayList<>();
    private static final List<Runnable> listeners = new UnorderedArrayList<>();

    public static final RainbowColor GLOBAL = new RainbowColor();

    private RainbowColors() {
    }

    @PostInit
    public static void init() {
        MotorClient.EVENT_BUS.subscribe(RainbowColors.class);
    }

    public static void addSetting(Setting<SettingColor> setting) {
        colorSettings.add(setting);
    }

    public static void addSettingList(Setting<List<SettingColor>> setting) {
        colorListSettings.add(setting);
    }

    public static void removeSetting(Setting<SettingColor> setting) {
        colorSettings.remove(setting);
    }

    public static void removeSettingList(Setting<List<SettingColor>> setting) {
        colorListSettings.remove(setting);
    }

    public static void add(SettingColor color) {
        colors.add(color);
    }

    public static void register(Runnable runnable) {
        listeners.add(runnable);
    }

    @EventHandler
    private static void onTick(TickEvent.Post event) {
        GLOBAL.setSpeed(Config.get().rainbowSpeed.get() / 100);
        GLOBAL.getNext();

        for (Setting<SettingColor> setting : colorSettings) {
            if (setting.module == null || setting.module.isActive()) setting.get().update();
        }

        for (Setting<List<SettingColor>> setting : colorListSettings) {
            if (setting.module == null || setting.module.isActive()) {
                for (SettingColor color : setting.get()) color.update();
            }
        }

        for (SettingColor color : colors) {
            color.update();
        }

        for (Waypoint waypoint : Waypoints.get()) {
            waypoint.color.get().update();
        }

        if (mc.currentScreen instanceof WidgetScreen) {
            for (SettingGroup group : GuiThemes.get().settings) {
                for (Setting<?> setting : group) {
                    if (setting instanceof ColorSetting) ((SettingColor) setting.get()).update();
                }
            }
        }

        for (Runnable listener : listeners) listener.run();
    }
}
