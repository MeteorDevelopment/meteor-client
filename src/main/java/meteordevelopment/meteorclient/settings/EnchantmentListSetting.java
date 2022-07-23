/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
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
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class EnchantmentListSetting extends Setting<List<Enchantment>> {
    public EnchantmentListSetting(String name, String description, List<Enchantment> defaultValue, Consumer<List<Enchantment>> onChanged, Consumer<Setting<List<Enchantment>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    public void resetImpl() {
        value = new ArrayList<>(defaultValue);
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
    public NbtCompound save(NbtCompound tag) {
        NbtList valueTag = new NbtList();
        for (Enchantment ench : get()) {
            Identifier id = Registry.ENCHANTMENT.getId(ench);
            if (id != null) valueTag.add(NbtString.of(id.toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<Enchantment> load(NbtCompound tag) {
        get().clear();

        NbtList valueTag = tag.getList("value", 8);
        for (NbtElement tagI : valueTag) {
            Enchantment enchantment = Registry.ENCHANTMENT.get(new Identifier(tagI.asString()));
            if (enchantment != null) get().add(enchantment);
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, List<Enchantment>, EnchantmentListSetting> {
        public Builder() {
            super(new ArrayList<>(0));
        }

        public Builder defaultValue(Enchantment... defaults) {
            return defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
        }

        @Override
        public EnchantmentListSetting build() {
            return new EnchantmentListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
