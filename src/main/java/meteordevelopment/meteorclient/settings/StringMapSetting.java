/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtString;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class StringMapSetting extends Setting<Map<String, String>> {
    public final Class<? extends WTextBox.Renderer> renderer;

    public StringMapSetting(String name, String description, Map<String, String> defaultValue, Consumer<Map<String, String>> onChanged, Consumer<Setting<Map<String, String>>> onModuleActivated, IVisible visible, Class<? extends WTextBox.Renderer> renderer) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.renderer = renderer;
    }

    @Override
    protected Map<String, String> parseImpl(String str) {
        String[] values = str.split(",");

        Map<String, String> map = new LinkedHashMap<>(values.length / 2);

        try {
            String left = null;
            for (int i = 0; i < values.length; i++) {
                if (i % 2 == 0) {
                    left = values[i];
                } else {
                    map.put(left, values[i]);
                }
            }
        } catch (Exception ignored) {
        }

        return map;
    }

    @Override
    protected boolean isValueValid(Map<String, String> value) {
        return true;
    }

    @Override
    protected void resetImpl() {
        value = new LinkedHashMap<>(defaultValue);
    }

    @Override
    protected NbtCompound save(NbtCompound tag) {
        NbtCompound valueTag = new NbtCompound();
        for (String key : get().keySet()) {
            valueTag.put(key, NbtString.of(get().get(key)));
        }
        tag.put("map", valueTag);

        return tag;
    }

    @Override
    protected Map<String, String> load(NbtCompound tag) {
        get().clear();

        NbtCompound valueTag = tag.getCompound("map");
        for (String key : valueTag.getKeys()) {
            get().put(key, valueTag.getString(key));
        }

        return get();
    }

    public static void fillTable(GuiTheme theme, WTable table, StringMapSetting setting) {
        table.clear();

        Map<String, String> map = setting.get();

        for (String key : map.keySet()) {
            AtomicReference<String> key2 = new AtomicReference<>(key);

            WTextBox textBoxK = table.add(theme.textBox(key)).minWidth(150).expandX().widget();
            textBoxK.actionOnUnfocused = () -> {
                String text = textBoxK.get();
                if (map.containsKey(text)) return;
                String value = map.remove(key);
                key2.set(text);
                map.put(text, value);
            };

            WTextBox textBoxV = table.add(theme.textBox(map.get(key), (text1, c) -> true, setting.renderer)).minWidth(150).expandX().widget();
            textBoxV.actionOnUnfocused = () -> map.replace(key2.get(), textBoxV.get());

            WMinus delete = table.add(theme.minus()).widget();
            delete.action = () -> {
                map.remove(key);
                fillTable(theme, table, setting);
            };

            table.row();
        }

        if (!map.isEmpty()) {
            table.add(theme.horizontalSeparator()).expandX();
            table.row();
        }

        WButton reset = table.add(theme.button(GuiRenderer.RESET)).widget();
        reset.action = () -> {
            setting.reset();
            fillTable(theme, table, setting);
        };

        WPlus add = table.add(theme.plus()).widget();
        add.action = () -> {
            map.put("", "");
            fillTable(theme, table, setting);
        };

        table.row();
    }

    public static class Builder extends SettingBuilder<Builder, Map<String, String>, StringMapSetting> {
        private Class<? extends WTextBox.Renderer> renderer;

        public Builder() {
            super(new LinkedHashMap<>(0));
        }

        public Builder defaultValue(Map<String, String> map) {
            this.defaultValue = map;
            return this;
        }

        public Builder renderer(Class<? extends WTextBox.Renderer> renderer) {
            this.renderer = renderer;
            return this;
        }

        @Override
        public StringMapSetting build() {
            return new StringMapSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, renderer);
        }
    }
}
