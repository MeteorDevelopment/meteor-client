/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import meteordevelopment.meteorclient.utils.misc.IChangeable;
import meteordevelopment.meteorclient.utils.misc.ICopyable;
import meteordevelopment.meteorclient.utils.misc.IGetter;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class EntityTypeDataSetting<T extends ICopyable<T> & ISerializable<T> & IChangeable & IEntityTypeData<T>> extends Setting<Map<EntityType, T>> {
    public final IGetter<T> defaultData;

    public EntityTypeDataSetting(String name, String description, Map<EntityType, T> defaultValue, Consumer<Map<EntityType, T>> onChanged, Consumer<Setting<Map<EntityType, T>>> onModuleActivated, IGetter<T> defaultData, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.defaultData = defaultData;
    }

    @Override
    public void resetImpl() {
        value = new HashMap<>(defaultValue);
    }

    @Override
    protected Map<EntityType, T> parseImpl(String str) {
        return new HashMap<>(0);
    }

    @Override
    protected boolean isValueValid(Map<EntityType, T> value) {
        return true;
    }

    @Override
    protected NbtCompound save(NbtCompound tag) {
        NbtCompound valueTag = new NbtCompound();
        for (EntityType entityType : get().keySet()) {
            valueTag.put(Registries.ENTITY_TYPE.getId(entityType).toString(), get().get(entityType).toTag());
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    protected Map<EntityType, T> load(NbtCompound tag) {
        get().clear();

        NbtCompound valueTag = tag.getCompound("value");
        for (String key : valueTag.getKeys()) {
            get().put(Registries.ENTITY_TYPE.get(new Identifier(key)), defaultData.get().copy().fromTag(valueTag.getCompound(key)));
        }

        return get();
    }

    public static class Builder<T extends ICopyable<T> & ISerializable<T> & IChangeable & IEntityTypeData<T>> extends SettingBuilder<Builder<T>, Map<EntityType, T>, EntityTypeDataSetting<T>> {
        private IGetter<T> defaultData;

        public Builder() {
            super(new HashMap<>(0));
        }

        public Builder<T> defaultData(IGetter<T> defaultData) {
            this.defaultData = defaultData;
            return this;
        }

        @Override
        public EntityTypeDataSetting<T> build() {
            return new EntityTypeDataSetting<>(name, description, defaultValue, onChanged, onModuleActivated, defaultData, visible);
        }
    }
}
