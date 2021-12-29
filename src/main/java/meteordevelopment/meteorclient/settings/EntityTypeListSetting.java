/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.function.Consumer;

// TODO: Change onlyAttackable to a filter
public class EntityTypeListSetting extends Setting<Object2BooleanMap<EntityType<?>>> {
    public final boolean onlyAttackable;

    public EntityTypeListSetting(String name, String description, Object2BooleanMap<EntityType<?>> defaultValue, Consumer<Object2BooleanMap<EntityType<?>>> onChanged, Consumer<Setting<Object2BooleanMap<EntityType<?>>>> onModuleActivated, IVisible visible, boolean onlyAttackable) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.onlyAttackable = onlyAttackable;
    }

    @Override
    public void resetImpl() {
        value = new Object2BooleanOpenHashMap<>(defaultValue);
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
    public NbtCompound save(NbtCompound tag) {
        NbtList valueTag = new NbtList();
        for (EntityType<?> entityType : get().keySet()) {
            if (get().getBoolean(entityType)) {
                valueTag.add(NbtString.of(Registry.ENTITY_TYPE.getId(entityType).toString()));
            }
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public Object2BooleanMap<EntityType<?>> load(NbtCompound tag) {
        get().clear();

        NbtList valueTag = tag.getList("value", 8);
        for (NbtElement tagI : valueTag) {
            EntityType<?> type = Registry.ENTITY_TYPE.get(new Identifier(tagI.asString()));
            if (!onlyAttackable || EntityUtils.isAttackable(type)) get().put(type, true);
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, Object2BooleanMap<EntityType<?>>, EntityTypeListSetting> {
        private boolean onlyAttackable = false;

        public Builder() {
            super(new Object2BooleanOpenHashMap<>(0));
        }

        public Builder defaultValue(EntityType<?>... defaults) {
            return defaultValue(defaults != null ? Utils.asO2BMap(defaults) : new Object2BooleanOpenHashMap<>(0));
        }

        public Builder onlyAttackable() {
            onlyAttackable = true;
            return this;
        }

        @Override
        public EntityTypeListSetting build() {
            return new EntityTypeListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, onlyAttackable);
        }
    }
}
