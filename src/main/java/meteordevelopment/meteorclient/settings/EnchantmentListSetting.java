/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EnchantmentListSetting extends Setting<List<Enchantment>> {
    public EnchantmentListSetting(String name, String description, List<Enchantment> defaultValue, Consumer<List<Enchantment>> onChanged, Consumer<Setting<List<Enchantment>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

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
    protected boolean isValueValid(List<Enchantment> value) {
        return true;
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registry.ENCHANTMENT.getIds();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = saveGeneral();

        NbtList valueTag = new NbtList();
        for (Enchantment ench : get()) {
            Identifier id = Registry.ENCHANTMENT.getId(ench);
            if (id != null) valueTag.add(NbtString.of(id.toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<Enchantment> fromTag(NbtCompound tag) {
        get().clear();

        NbtList valueTag = tag.getList("value", 8);
        for (NbtElement tagI : valueTag) {
            Enchantment enchantment = Registry.ENCHANTMENT.get(new Identifier(tagI.asString()));
            if (enchantment != null) get().add(enchantment);
        }

        changed();
        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private List<Enchantment> defaultValue;
        private Consumer<List<Enchantment>> onChanged;
        private Consumer<Setting<List<Enchantment>>> onModuleActivated;
        private IVisible visible;

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

        public Builder visible(IVisible visible) {
            this.visible = visible;
            return this;
        }

        public EnchantmentListSetting build() {
            return new EnchantmentListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
