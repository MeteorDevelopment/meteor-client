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
import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public class BaritoneTab extends Tab {
    private static Settings settings;

    public BaritoneTab() {
        super("Baritone");
    }

    @SuppressWarnings("unchecked")
    private static Settings getSettings() {
        if (settings != null) return settings;

        settings = new Settings();

        SettingGroup sgBool = settings.createGroup("Checkboxes");
        SettingGroup sgDouble = settings.createGroup("Numbers");
        SettingGroup sgInt = settings.createGroup("Whole Numbers");
        SettingGroup sgString = settings.createGroup("Strings");
        SettingGroup sgColor = settings.createGroup("Colors");

        SettingGroup sgBlockLists = settings.createGroup("Block Lists");
        SettingGroup sgItemLists = settings.createGroup("Item Lists");

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
                } else if (value instanceof String) {
                    sgString.add(new StringSetting.Builder()
                        .name(setting.getName())
                        .description(setting.getName())
                        .defaultValue((String) setting.defaultValue)
                        .onChanged(string -> setting.value = string)
                        .onModuleActivated(stringSetting -> stringSetting.set((String) setting.value))
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
                } else if (value instanceof List) {
                    Type listType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                    Type type = ((ParameterizedType) listType).getActualTypeArguments()[0];

                    if (type == Block.class) {
                        sgBlockLists.add(new BlockListSetting.Builder()
                            .name(setting.getName())
                            .description(setting.getName())
                            .defaultValue((List<Block>) setting.defaultValue)
                            .onChanged(blockList -> setting.value = blockList)
                            .onModuleActivated(blockListSetting -> blockListSetting.set((List<Block>) setting.value))
                            .build()
                        );
                    } else if (type == Item.class) {
                        sgItemLists.add(new ItemListSetting.Builder()
                            .name(setting.getName())
                            .description(setting.getName())
                            .defaultValue((List<Item>) setting.defaultValue)
                            .onChanged(itemList -> setting.value = itemList)
                            .onModuleActivated(itemListSetting -> itemListSetting.set((List<Item>) setting.value))
                            .build()
                        );
                    }
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
