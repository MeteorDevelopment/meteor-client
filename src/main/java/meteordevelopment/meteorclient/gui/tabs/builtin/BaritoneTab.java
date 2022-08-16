/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.tabs.builtin;

import baritone.api.BaritoneAPI;
import baritone.api.utils.SettingsUtil;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.screen.Screen;

import java.awt.*;
import java.lang.reflect.Field;

public class BaritoneTab extends Tab {
    private static Settings settings;

    public BaritoneTab() {
        super("Baritone");
    }

    private static Settings getSettings() {
        if (settings != null) return settings;

        settings = new Settings();

        SettingGroup sgBool = settings.createGroup("Checkboxes");
        SettingGroup sgDouble = settings.createGroup("Numbers");
        SettingGroup sgInt = settings.createGroup("Whole Numbers");
        SettingGroup sgColor = settings.createGroup("Colors");

        try {
            Class<? extends baritone.api.Settings> klass = BaritoneAPI.getSettings().getClass();
            for (Field field : klass.getDeclaredFields()) {
                Object obj = field.get(BaritoneAPI.getSettings());
                if (!(obj instanceof baritone.api.Settings.Setting setting)) continue;

                Object value = setting.value;

                if (value instanceof Boolean) {
                    sgBool.add(new BoolSetting.Builder()
                        .name(setting.getName())
                        .description(setting.getName())
                        .defaultValue((boolean) setting.defaultValue)
                        .onChanged(aBoolean -> setting.value = aBoolean)
                        .onModuleActivated(booleanSetting -> booleanSetting.set((Boolean) setting.value))
                        .build()
                    );
                } else if (value instanceof Double) {
                    sgDouble.add(new DoubleSetting.Builder()
                        .name(setting.getName())
                        .description(setting.getName())
                        .defaultValue((double) setting.defaultValue)
                        .onChanged(aDouble -> setting.value = aDouble)
                        .onModuleActivated(doubleSetting -> doubleSetting.set((Double) setting.value))
                        .build()
                    );
                } else if (value instanceof Float) {
                    sgDouble.add(new DoubleSetting.Builder()
                        .name(setting.getName())
                        .description(setting.getName())
                        .defaultValue(((Float) setting.defaultValue).doubleValue())
                        .onChanged(aDouble -> setting.value = aDouble.floatValue())
                        .onModuleActivated(doubleSetting -> doubleSetting.set(((Float) setting.value).doubleValue()))
                        .build()
                    );
                } else if (value instanceof Integer) {
                    sgInt.add(new IntSetting.Builder()
                        .name(setting.getName())
                        .description(setting.getName())
                        .defaultValue((int) setting.defaultValue)
                        .onChanged(integer -> setting.value = integer)
                        .onModuleActivated(integerSetting -> integerSetting.set((Integer) setting.value))
                        .build()
                    );
                } else if (value instanceof Long) {
                    sgInt.add(new IntSetting.Builder()
                        .name(setting.getName())
                        .description(setting.getName())
                        .defaultValue(((Long) setting.defaultValue).intValue())
                        .onChanged(integer -> setting.value = integer.longValue())
                        .onModuleActivated(integerSetting -> integerSetting.set(((Long) setting.value).intValue()))
                        .build()
                    );
                } else if (value instanceof Color) {
                    Color c = (Color) setting.value;

                    sgColor.add(new ColorSetting.Builder()
                        .name(setting.getName())
                        .description(setting.getName())
                        .defaultValue(new SettingColor(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()))
                        .onChanged(color -> setting.value = new Color(color.r, color.g, color.b, color.a))
                        .onModuleActivated(colorSetting -> colorSetting.set(new SettingColor(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha())))
                        .build()
                    );
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return settings;
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new BaritoneScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof BaritoneScreen;
    }

    private static class BaritoneScreen extends WindowTabScreen {
        public BaritoneScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);

            getSettings().onActivated();
        }

        @Override
        public void initWidgets() {
            WTextBox filter = add(theme.textBox("")).minWidth(400).expandX().widget();
            filter.setFocused(true);
            filter.action = () -> {
                clear();

                add(filter);
                add(theme.settings(getSettings(), filter.get().trim())).expandX();
            };

            add(theme.settings(getSettings(), filter.get().trim())).expandX();
        }

        @Override
        protected void onClosed() {
            SettingsUtil.save(BaritoneAPI.getSettings());
        }
    }
}
