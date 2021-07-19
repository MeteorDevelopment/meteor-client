/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class StringListSetting extends Setting<List<String>>{

    public String newText = "";
    public StringListSetting(String name, String description, List<String> defaultValue, Consumer<List<String>> onChanged, Consumer<Setting<List<String>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    protected List<String> parseImpl(String str) {
        return Arrays.asList(str.split(","));
    }

    @Override
    protected boolean isValueValid(List<String> value) {
        return true;
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        NbtList valueTag = new NbtList();
        for (int i = 0; i < this.value.size(); i++) {
            valueTag.add(i, NbtString.of(get().get(i)));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<String> fromTag(NbtCompound tag) {
        get().clear();

        NbtList valueTag = tag.getList("value", 8);
        for (NbtElement tagI : valueTag) {
            get().add(tagI.asString());
        }

        changed();
        return get();
    }

    @Override
    public void reset(boolean callbacks) {
        value = new ArrayList<>(defaultValue);
        if (callbacks) changed();
    }

    public static void fillTable(GuiTheme theme, WTable table, StringListSetting setting) {
        setting.get().removeIf(String::isEmpty);
        ArrayList<String> strings = new ArrayList<>(setting.get());

        for (int i = 0; i < setting.get().size(); i++) {
            int msgI = i;
            String message = setting.get().get(i);

            WTextBox textBox = table.add(theme.textBox(message)).expandX().widget();
            textBox.action = () -> {
                strings.set(msgI, textBox.get());
                setting.set(strings);

                table.clear();
                fillTable(theme, table, setting);
            };

            WMinus delete = table.add(theme.minus()).widget();
            delete.action = () -> {
                strings.remove(msgI);
                setting.set(strings);

                table.clear();
                fillTable(theme, table, setting);
            };

            table.row();
        }

        WTextBox textBox = table.add(theme.textBox(setting.newText)).minWidth(300).expandX().widget();
        textBox.action = () -> setting.newText = textBox.get();

        WPlus add = table.add(theme.plus()).widget();
        add.action = () -> {
            strings.add(setting.newText);
            setting.set(strings);
            setting.newText = "";

            table.clear();
            fillTable(theme, table, setting);
        };

        // Reset
        table.row();
        WButton reset = table.add(theme.button("Reset")).widget();
        reset.action = () -> {
            setting.reset();
            table.clear();
            fillTable(theme, table, setting);
        };
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private List<String> defaultValue;
        private Consumer<List<String>> onChanged;
        private Consumer<Setting<List<String>>> onModuleActivated;
        private IVisible visible;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(List<String> defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<List<String>> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<List<String>>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public Builder visible(IVisible visible) {
            this.visible = visible;
            return this;
        }

        public StringListSetting build() {
            return new StringListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
