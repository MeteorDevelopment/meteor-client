/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.gui.screens.topbar;

import baritone.api.BaritoneAPI;
import baritone.api.utils.SettingsUtil;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.render.color.SettingColor;

import java.awt.*;
import java.lang.reflect.Field;

public class TopBarBaritone extends TopBarWindowScreen {
    public TopBarBaritone() {
        super(TopBarType.Baritone);
    }

    @Override
    protected void initWidgets() {
        Settings s = new Settings();

        SettingGroup sgBool = s.createGroup("Checkboxes");
        SettingGroup sgDouble = s.createGroup("Numbers");
        SettingGroup sgInt = s.createGroup("Whole Numbers");
        SettingGroup sgColor = s.createGroup("Colors");

        try {
            Class<? extends baritone.api.Settings> klass = BaritoneAPI.getSettings().getClass();
            for (Field field : klass.getDeclaredFields()) {
                Object obj = field.get(BaritoneAPI.getSettings());
                if (!(obj instanceof baritone.api.Settings.Setting)) continue;

                baritone.api.Settings.Setting setting = (baritone.api.Settings.Setting<?>) obj;
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

        add(s.createTable()).fillX().expandX();
    }

    @Override
    protected void onClosed() {
        SettingsUtil.save(BaritoneAPI.getSettings());
    }
}
