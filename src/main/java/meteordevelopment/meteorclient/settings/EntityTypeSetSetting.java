/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import meteordevelopment.meteorclient.settings.groups.GroupSet;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class EntityTypeSetSetting extends GroupedSetSetting<EntityType<?>> {
    public static Groups<EntityType<?>> GROUPS = new Groups<>();

    public EntityTypeSetSetting(String name, String description, GroupSet<EntityType<?>, Groups<EntityType<?>>.Group> defaultValue, Predicate<EntityType<?>> filter, Consumer<GroupSet<EntityType<?>, Groups<EntityType<?>>.Group>> onChanged, Consumer<Setting<GroupSet<EntityType<?>, Groups<EntityType<?>>.Group>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, filter, onChanged, onModuleActivated, visible);
    }

    @Override
    public EntityType<?> parseItem(String str) {
        return parseId(Registries.ENTITY_TYPE, str);
    }

    @Override
    public NbtElement itemToNbt(EntityType<?> entityType) {
        return NbtString.of(Registries.ENTITY_TYPE.getId(entityType).toString());
    }

    @Override
    public EntityType<?> itemFromNbt(NbtElement e) {
        return Registries.ENTITY_TYPE.get(Identifier.of(e.asString().orElse("")));
    }

    @Override
    protected Groups<EntityType<?>> groups() {
        return GROUPS;
    }

    @Override
    public void buildSuggestions(List<String> to) {
        for (EntityType<?> entityType : Registries.ENTITY_TYPE) {
            if (filter == null || filter.test(entityType)) to.add(Registries.ENTITY_TYPE.getId(entityType).toString());
        }
    }

    public static class Builder extends SettingBuilder<Builder, GroupSet<EntityType<?>, Groups<EntityType<?>>.Group>, EntityTypeSetSetting> {
        private Predicate<EntityType<?>> filter;

        public Builder() {
            super(new GroupSet<>());
        }

        public Builder defaultValue(EntityType<?>... defaults) {
            return defaultValue(defaults != null ? new GroupSet<>(Arrays.asList(defaults)) : new GroupSet<>());
        }

        public Builder onlyAttackable() {
            filter = EntityUtils::isAttackable;
            return this;
        }

        public Builder filter(Predicate<EntityType<?>> filter){
            this.filter = filter;
            return this;
        }

        @Override
        public EntityTypeSetSetting build() {
            return new EntityTypeSetSetting(name, description, defaultValue, filter, onChanged, onModuleActivated, visible);
        }
    }

    static Groups<EntityType<?>>.Group ANIMAL, WATER_ANIMAL, MONSTER, AMBIENT, MISC;

    static {
        ANIMAL = GROUPS.dynamic("animals").get();
        WATER_ANIMAL = GROUPS.dynamic("water-animals").get();
        MONSTER = GROUPS.dynamic("monsters").get();
        AMBIENT = GROUPS.dynamic("ambient").get();
        MISC = GROUPS.dynamic("misc").get();

        for (EntityType<?> entityType : Registries.ENTITY_TYPE) {
            if (entityType.getSpawnGroup() == SpawnGroup.CREATURE) ANIMAL.add(entityType);
            if (entityType.getSpawnGroup() == SpawnGroup.MONSTER) MONSTER.add(entityType);
            if (entityType.getSpawnGroup() == SpawnGroup.AMBIENT) AMBIENT.add(entityType);
            if (entityType.getSpawnGroup() == SpawnGroup.MISC) MISC.add(entityType);
            if (entityType.getSpawnGroup() == SpawnGroup.WATER_AMBIENT
                || entityType.getSpawnGroup() == SpawnGroup.WATER_CREATURE
                || entityType.getSpawnGroup() == SpawnGroup.UNDERGROUND_WATER_CREATURE
                || entityType.getSpawnGroup() == SpawnGroup.AXOLOTLS) WATER_ANIMAL.add(entityType);
        }
    }
}
