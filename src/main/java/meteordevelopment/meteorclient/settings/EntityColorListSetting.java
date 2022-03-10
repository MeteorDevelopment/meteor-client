/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import meteordevelopment.meteorclient.utils.render.color.SettingBooleanColor;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.function.Consumer;

public class EntityColorListSetting extends Setting<Object2ObjectMap<EntityType<?>, SettingBooleanColor>> {


    public EntityColorListSetting(String name, String description, Object2ObjectMap<EntityType<?>, SettingBooleanColor> defaultValue, Consumer<Object2ObjectMap<EntityType<?>, SettingBooleanColor>> onChanged, Consumer<Setting<Object2ObjectMap<EntityType<?>, SettingBooleanColor>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    public boolean isActive(EntityType<?> entityType) {
        get().putIfAbsent(entityType, new SettingBooleanColor());
        return get().get(entityType).isActive();
    }

    public boolean deactivate(EntityType<?> entityType) { // Like the remove boolean from the original
        if (isActive(entityType)) {
            get().get(entityType).setActive(false);
            return true;
        }
        return false;
    }

    public void activate(EntityType<?> entityType) {
        get().putIfAbsent(entityType, new SettingBooleanColor());
        get().get(entityType).setActive(true);
    }

    @Override
    public void resetImpl() {
        value = new Object2ObjectOpenHashMap<>(defaultValue);
    }

    @Override
    protected Object2ObjectMap<EntityType<?>, SettingBooleanColor> parseImpl(String str) {
        String[] values = str.split(";");
        Object2ObjectMap<EntityType<?>, SettingBooleanColor> entities = new Object2ObjectOpenHashMap<>(values.length);

        try {
            for (String value : values) {
                String[] valuez = value.split(",");
                String[] colors = value.split(" ");

                EntityType<?> entity = parseId(Registry.ENTITY_TYPE, valuez[0]);
                if (entity == null) continue;
                SettingBooleanColor color = new SettingBooleanColor(new SettingColor(Integer.parseInt(colors[0]), Integer.parseInt(colors[1]), Integer.parseInt(colors[2]), Integer.parseInt(colors[3])), Boolean.parseBoolean(colors[4]));
                entities.put(entity, color);
            }
        } catch (Exception ignored) {
            return null;
        }

        return entities;
    }

    @Override
    protected boolean isValueValid(Object2ObjectMap<EntityType<?>, SettingBooleanColor> value) {
        return true;
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registry.ENTITY_TYPE.getIds();
    }

    @Override
    public NbtCompound save(NbtCompound tag) {
        NbtCompound valueTag = new NbtCompound();

        for (EntityType<?> entityType : get().keySet()) {
            if (get().get(entityType).isActive() || get().get(entityType).getColor() != null) {
                valueTag.put(Registry.ENTITY_TYPE.getId(entityType).toString(), get().get(entityType).toTag());
            }
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public Object2ObjectMap<EntityType<?>, SettingBooleanColor> load(NbtCompound tag) {
        get().clear();

        NbtCompound valueTag = tag.getCompound("value");

        for (String tagI : valueTag.getKeys()) {
            get().put(Registry.ENTITY_TYPE.get(new Identifier(tagI)), new SettingBooleanColor().fromTag(valueTag.getCompound(tagI)));
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, Object2ObjectMap<EntityType<?>, SettingBooleanColor>, EntityColorListSetting> {

        public Builder() {
            super(new Object2ObjectOpenHashMap<>(0));
        }

        @Override
        public EntityColorListSetting build() {
            return new EntityColorListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
