/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.lang.reflect.AccessFlag;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class EnchantmentListSetting extends Setting<Set<ResourceKey<Enchantment>>> {
    public EnchantmentListSetting(String name, String description, Set<ResourceKey<Enchantment>> defaultValue, Consumer<Set<ResourceKey<Enchantment>>> onChanged, Consumer<Setting<Set<ResourceKey<Enchantment>>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    public void resetImpl() {
        value = new ObjectOpenHashSet<>(defaultValue);
    }

    @Override
    protected Set<ResourceKey<Enchantment>> parseImpl(String str) {
        String[] values = str.split(",");
        Set<ResourceKey<Enchantment>> enchs = new ObjectOpenHashSet<>(values.length);

        for (String value : values) {
            String name = value.trim();

            Identifier id;
            if (name.contains(":")) id = Identifier.parse(name);
            else id = Identifier.withDefaultNamespace(name);

            enchs.add(ResourceKey.create(Registries.ENCHANTMENT, id));
        }

        return enchs;
    }

    @Override
    protected boolean isValueValid(Set<ResourceKey<Enchantment>> value) {
        return true;
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Optional.ofNullable(Minecraft.getInstance().getConnection())
            .flatMap(networkHandler -> networkHandler.registryAccess().lookup(Registries.ENCHANTMENT))
            .map(Registry::keySet).orElse(Set.of());
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag valueTag = new ListTag();
        for (ResourceKey<Enchantment> ench : get()) {
            valueTag.add(StringTag.valueOf(ench.identifier().toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public Set<ResourceKey<Enchantment>> load(CompoundTag tag) {
        get().clear();

        for (Tag tagI : tag.getListOrEmpty("value")) {
            get().add(ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse(tagI.asString().orElse(""))));
        }

        return get();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static class Builder extends SettingBuilder<Builder, Set<ResourceKey<Enchantment>>, EnchantmentListSetting> {
        private static final Set<ResourceKey<Enchantment>> VANILLA_DEFAULTS;

        public Builder() {
            super(new ObjectOpenHashSet<>());
        }

        public Builder vanillaDefaults() {
            return defaultValue(VANILLA_DEFAULTS);
        }

        @SafeVarargs
        public final Builder defaultValue(ResourceKey<Enchantment>... defaults) {
            return defaultValue(defaults != null ? new ObjectOpenHashSet<>(defaults) : new ObjectOpenHashSet<>());
        }

        @Override
        public EnchantmentListSetting build() {
            return new EnchantmentListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }

        static {
            VANILLA_DEFAULTS = (Set) Arrays.stream(Enchantments.class.getDeclaredFields())
                .filter(field -> field.accessFlags().containsAll(List.of(AccessFlag.PUBLIC, AccessFlag.STATIC, AccessFlag.FINAL)))
                .filter(field -> field.getType() == ResourceKey.class)
                .map(field -> {
                    try {
                        return field.get(null);
                    } catch (IllegalAccessException e) {
                        return null;
                    }
                }).filter(Objects::nonNull)
                .map(ResourceKey.class::cast)
                .filter(registryKey -> registryKey.registryKey() == Registries.ENCHANTMENT)
                .collect(Collectors.toSet());
        }
    }
}
