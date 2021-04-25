/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.settings;

//Created by squidoodly 25/07/2020

import net.minecraft.enchantment.Enchantment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EnchListSetting extends Setting<List<Enchantment>>{
    public EnchListSetting(String name, String description, List<Enchantment> defaultValue, Consumer<List<Enchantment>> onChanged, Consumer<Setting<List<Enchantment>>> onModuleActivated){
        super(name, description, defaultValue, onChanged, onModuleActivated);

        value = new ArrayList<>(defaultValue);
    }

    @Override
    public void reset(boolean callbacks) {
        value = new ArrayList<>(defaultValue);
        if (callbacks) changed();
    }

    @Override
    protected List<Enchantment> parseImpl(String str) {
        String[] values = str.split(",");
        List<Enchantment> enchs = new ArrayList<>(values.length);

        try {
            for (String value : values) {
                Enchantment ench = parseId(Registry.ENCHANTMENT, value);
                if (ench != null) enchs.add(ench);
            }
        } catch (Exception ignored) {}

        return enchs;
    }

    @Override
    protected boolean isValueValid(List<Enchantment> value) { return true; }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registry.ENCHANTMENT.getIds();
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = saveGeneral();

        ListTag valueTag = new ListTag();
        for(Enchantment ench : get()) {
            try {
                valueTag.add(StringTag.of(Registry.ENCHANTMENT.getId(ench).toString()));
            } catch (NullPointerException ignored) {
                //Cringe. Idk what's going on but it crashed me so...
            }
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<Enchantment> fromTag(CompoundTag tag) {
        get().clear();

        ListTag valueTag = tag.getList("value", 8);
        for (Tag tag1 : valueTag) {
            get().add(Registry.ENCHANTMENT.get(new Identifier(tag1.asString())));
        }

        changed();
        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private List<Enchantment> defaultValue;
        private Consumer<List<Enchantment>> onChanged;
        private Consumer<Setting<List<Enchantment>>> onModuleActivated;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(List<Enchantment> defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<List<Enchantment>> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<List<Enchantment>>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public EnchListSetting build() {
            return new EnchListSetting(name, description, defaultValue, onChanged, onModuleActivated);
        }
    }
}
