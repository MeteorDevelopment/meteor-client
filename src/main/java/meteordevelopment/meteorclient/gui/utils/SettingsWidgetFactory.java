/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.utils;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.Settings;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class SettingsWidgetFactory {
    private static final Map<Class<?>, Function<GuiTheme, SettingsWidgetFactory.Factory>> customFactories = new HashMap<>();

    protected final GuiTheme theme;
    protected final Map<Class<?>, Factory> factories = new HashMap<>();

    public SettingsWidgetFactory(GuiTheme theme) {
        this.theme = theme;
    }

    /** {@code SettingsWidgetFactory.registerCustomFactory(SomeSetting.class, (theme) -> (table, setting) -> {//create widget})} */
    public static void registerCustomFactory(Class<?> settingClass, Function<GuiTheme, Factory> factoryFunction) {
        customFactories.put(settingClass, factoryFunction);
    }

    public static void unregisterCustomFactory(Class<?> settingClass) {
        customFactories.remove(settingClass);
    }

    public abstract WWidget create(GuiTheme theme, Settings settings, String filter);

    protected Factory getFactory(Class<?> settingClass) {
        if (customFactories.containsKey(settingClass)) return customFactories.get(settingClass).apply(theme);
        return factories.get(settingClass);
    }

    @FunctionalInterface
    public interface Factory {
        void create(WTable table, Setting<?> setting);
    }
}
