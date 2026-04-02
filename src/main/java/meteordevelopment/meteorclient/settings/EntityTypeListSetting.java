/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class EntityTypeListSetting extends Setting<Set<EntityType<?>>> {
    public final Predicate<EntityType<?>> filter;
    private List<String> suggestions;
    private final static List<String> groups = List.of("animal", "wateranimal", "monster", "ambient", "misc");

    public EntityTypeListSetting(String name, String description, Set<EntityType<?>> defaultValue, Consumer<Set<EntityType<?>>> onChanged, Consumer<Setting<Set<EntityType<?>>>> onModuleActivated, IVisible visible, Predicate<EntityType<?>> filter) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.filter = filter;
    }

    @Override
    public void resetImpl() {
        value = new ObjectOpenHashSet<>(defaultValue);
    }

    @Override
    protected Set<EntityType<?>> parseImpl(String str) {
        String[] values = str.split(",");
        Set<EntityType<?>> entities = new ObjectOpenHashSet<>(values.length);

        try {
            for (String value : values) {
                EntityType<?> entity = parseId(BuiltInRegistries.ENTITY_TYPE, value);
                if (entity != null) entities.add(entity);
                else {
                    String lowerValue = value.trim().toLowerCase();
                    if (!groups.contains(lowerValue)) continue;

                    for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
                        if (filter != null && !filter.test(entityType)) continue;

                        switch (lowerValue) {
                            case "animal" -> {
                                if (entityType.getCategory() == MobCategory.CREATURE) entities.add(entityType);
                            }
                            case "wateranimal" -> {
                                if (entityType.getCategory() == MobCategory.WATER_AMBIENT
                                    || entityType.getCategory() == MobCategory.WATER_CREATURE
                                    || entityType.getCategory() == MobCategory.UNDERGROUND_WATER_CREATURE
                                    || entityType.getCategory() == MobCategory.AXOLOTLS) entities.add(entityType);
                            }
                            case "monster" -> {
                                if (entityType.getCategory() == MobCategory.MONSTER) entities.add(entityType);
                            }
                            case "ambient" -> {
                                if (entityType.getCategory() == MobCategory.AMBIENT) entities.add(entityType);
                            }
                            case "misc" -> {
                                if (entityType.getCategory() == MobCategory.MISC) entities.add(entityType);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return entities;
    }

    @Override
    protected boolean isValueValid(Set<EntityType<?>> value) {
        return true;
    }

    @Override
    public List<String> getSuggestions() {
        if (suggestions == null) {
            suggestions = new ArrayList<>(groups);
            for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
                if (filter == null || filter.test(entityType))
                    suggestions.add(BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString());
            }
        }

        return suggestions;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag valueTag = new ListTag();
        for (EntityType<?> entityType : get()) {
            valueTag.add(StringTag.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public Set<EntityType<?>> load(CompoundTag tag) {
        get().clear();

        ListTag valueTag = tag.getListOrEmpty("value");
        for (Tag tagI : valueTag) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(tagI.asString().orElse("")));
            if (filter == null || filter.test(type)) get().add(type);
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, Set<EntityType<?>>, EntityTypeListSetting> {
        private Predicate<EntityType<?>> filter;

        public Builder() {
            super(new ObjectOpenHashSet<>(0));
        }

        public Builder defaultValue(EntityType<?>... defaults) {
            return defaultValue(defaults != null ? new ObjectOpenHashSet<>(defaults) : new ObjectOpenHashSet<>(0));
        }

        public Builder onlyAttackable() {
            filter = EntityUtils::isAttackable;
            return this;
        }

        public Builder filter(Predicate<EntityType<?>> filter) {
            this.filter = filter;
            return this;
        }

        @Override
        public EntityTypeListSetting build() {
            return new EntityTypeListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, filter);
        }
    }
}
