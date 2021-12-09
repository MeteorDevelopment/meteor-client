/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.function.Consumer;

public class EntityTypeSetting extends Setting<EntityType<?>> {
    public final boolean onlyAttackable, onlyLiving;

    public EntityTypeSetting(String name, String description, EntityType<?> defaultValue, Consumer<EntityType<?>> onChanged, Consumer<Setting<EntityType<?>>> onModuleActivated, IVisible visible, boolean onlyAttackable, boolean onlyLiving) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.onlyAttackable = onlyAttackable;
        this.onlyLiving = onlyLiving;
    }

    @Override
    public EntityType<?> parseImpl(String str) {
        EntityType<?> entityType = null;

        try {
            entityType = parseId(Registry.ENTITY_TYPE, str);
        }
        catch (Exception ignored) {}
        return entityType;
    }

    @Override
    public boolean isValueValid(EntityType<?> value) {
        return true;
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registry.ENTITY_TYPE.getIds();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("value", Registry.ENTITY_TYPE.getId(get()).toString());
        return tag;
    }

    @Override
    public EntityType<?> fromTag(NbtCompound tag) {
        value = Registry.ENTITY_TYPE.get(new Identifier(tag.getString("value")));

        if (onlyAttackable && !EntityUtils.isAttackable(value)) {
            for (EntityType<?> entityType : Registry.ENTITY_TYPE) {
                if (EntityUtils.isAttackable(entityType)) {
                    value = entityType;
                    break;
                }
            }
        }

        if (onlyLiving && !EntityUtils.isLiving(value)) {
            for (EntityType<?> entityType : Registry.ENTITY_TYPE) {
                if (EntityUtils.isLiving(entityType)) {
                    value = entityType;
                    break;
                }
            }
        }

        onChanged();
        return get();
    }

    public static class Builder extends SettingBuilder<Builder, EntityType<?>, EntityTypeSetting> {
        private boolean onlyAttackable, onlyLiving;

        public Builder() {
            super(null);
        }

        public Builder onlyAttackable() {
            onlyAttackable = true;
            return this;
        }

        public Builder onlyLiving() {
            onlyLiving = true;
            return this;
        }

        @Override
        public EntityTypeSetting build() {
            return new EntityTypeSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, onlyAttackable, onlyLiving);
        }
    }
}
