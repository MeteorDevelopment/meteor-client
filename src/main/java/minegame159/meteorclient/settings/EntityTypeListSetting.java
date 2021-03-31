/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.settings;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import minegame159.meteorclient.utils.entity.EntityUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.function.Consumer;

public class EntityTypeListSetting extends Setting<Object2BooleanMap<EntityType<?>>> {
    public final boolean onlyAttackable;

    public EntityTypeListSetting(String name, String description, Object2BooleanMap<EntityType<?>> defaultValue, Consumer<Object2BooleanMap<EntityType<?>>> onChanged, Consumer<Setting<Object2BooleanMap<EntityType<?>>>> onModuleActivated, boolean onlyAttackable) {
        super(name, description, defaultValue, onChanged, onModuleActivated);

        this.onlyAttackable = onlyAttackable;
        value = new Object2BooleanOpenHashMap<>(defaultValue);
    }

    @Override
    public void reset(boolean callbacks) {
        value = new Object2BooleanOpenHashMap<>(defaultValue);
        if (callbacks) changed();
    }

    @Override
    protected Object2BooleanMap<EntityType<?>> parseImpl(String str) {
        String[] values = str.split(",");
        Object2BooleanMap<EntityType<?>> entities = new Object2BooleanOpenHashMap<>(values.length);

        try {
            for (String value : values) {
                EntityType<?> entity = parseId(Registry.ENTITY_TYPE, value);
                if (entity != null) entities.put(entity, true);
            }
        } catch (Exception ignored) {}

        return entities;
    }

    @Override
    protected boolean isValueValid(Object2BooleanMap<EntityType<?>> value) {
        return true;
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registry.ENTITY_TYPE.getIds();
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = saveGeneral();

        ListTag valueTag = new ListTag();
        for (EntityType<?> entityType : get().keySet()) {
            if (get().getBoolean(entityType)) {
                valueTag.add(StringTag.of(Registry.ENTITY_TYPE.getId(entityType).toString()));
            }
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public Object2BooleanMap<EntityType<?>> fromTag(CompoundTag tag) {
        get().clear();

        ListTag valueTag = tag.getList("value", 8);
        for (Tag tagI : valueTag) {
            EntityType<?> type = Registry.ENTITY_TYPE.get(new Identifier(tagI.asString()));
            if (!onlyAttackable || EntityUtils.isAttackable(type)) get().put(type, true);
        }

        changed();
        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private Object2BooleanMap<EntityType<?>> defaultValue;
        private Consumer<Object2BooleanMap<EntityType<?>>> onChanged;
        private Consumer<Setting<Object2BooleanMap<EntityType<?>>>> onModuleActivated;
        private boolean onlyAttackable = false;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(Object2BooleanMap<EntityType<?>> defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<Object2BooleanMap<EntityType<?>>> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<Object2BooleanMap<EntityType<?>>>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public Builder onlyAttackable() {
            onlyAttackable = true;
            return this;
        }

        public EntityTypeListSetting build() {
            return new EntityTypeListSetting(name, description, defaultValue, onChanged, onModuleActivated, onlyAttackable);
        }
    }
}
