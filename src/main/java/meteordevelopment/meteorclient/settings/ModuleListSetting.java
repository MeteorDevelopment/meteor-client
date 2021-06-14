/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ModuleListSetting extends Setting<List<Module>> {
    private static List<String> suggestions;

    public ModuleListSetting(String name, String description, List<Module> defaultValue, Consumer<List<Module>> onChanged, Consumer<Setting<List<Module>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        value = new ArrayList<>(defaultValue);
    }

    @Override
    public void reset(boolean callbacks) {
        value = new ArrayList<>(defaultValue);
        if (callbacks) changed();
    }

    @Override
    protected List<Module> parseImpl(String str) {
        String[] values = str.split(",");
        List<Module> modules = new ArrayList<>(values.length);

        try {
            for (String value : values) {
                Module module = Modules.get().get(value.trim());
                if (module != null) modules.add(module);
            }
        } catch (Exception ignored) {}

        return modules;
    }

    @Override
    protected boolean isValueValid(List<Module> value) {
        return true;
    }

    @Override
    public List<String> getSuggestions() {
        if (suggestions == null) {
            suggestions = new ArrayList<>(Modules.get().getAll().size());
            for (Module module : Modules.get().getAll()) suggestions.add(module.name);
        }

        return suggestions;
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = saveGeneral();

        NbtList modulesTag = new NbtList();
        for (Module module : get()) modulesTag.add(NbtString.of(module.name));
        tag.put("modules", modulesTag);

        return tag;
    }

    @Override
    public List<Module> fromTag(NbtCompound tag) {
        get().clear();

        NbtList valueTag = tag.getList("modules", 8);
        for (NbtElement tagI : valueTag) {
            Module module = Modules.get().get(tagI.asString());
            if (module != null) get().add(module);
        }

        changed();
        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private List<Module> defaultValue;
        private Consumer<List<Module>> onChanged;
        private Consumer<Setting<List<Module>>> onModuleActivated;
        private IVisible visible;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(List<Module> defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<List<Module>> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<List<Module>>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public Builder visible(IVisible visible) {
            this.visible = visible;
            return this;
        }

        public ModuleListSetting build() {
            return new ModuleListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
