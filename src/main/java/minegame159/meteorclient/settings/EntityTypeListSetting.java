/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.settings;

import minegame159.meteorclient.gui.screens.settings.EntityTypeListSettingScreen;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.utils.EntityUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EntityTypeListSetting extends Setting<List<EntityType<?>>> {
    public final boolean onlyAttackable;

    public EntityTypeListSetting(String name, String description, List<EntityType<?>> defaultValue, Consumer<List<EntityType<?>>> onChanged, Consumer<Setting<List<EntityType<?>>>> onModuleActivated, boolean onlyAttackable) {
        super(name, description, defaultValue, onChanged, onModuleActivated);

        this.onlyAttackable = onlyAttackable;
        value = new ArrayList<>(defaultValue);
        
        widget = new WButton("Select");
        ((WButton) widget).action = () -> MinecraftClient.getInstance().openScreen(new EntityTypeListSettingScreen(this));
    }

    @Override
    public void reset(boolean callbacks) {
        value = new ArrayList<>(defaultValue);
        if (callbacks) {
            resetWidget();
            changed();
        }
    }

    @Override
    protected List<EntityType<?>> parseImpl(String str) {
        String[] values = str.split(",");
        List<EntityType<?>> entities = new ArrayList<>(1);

        try {
            for (String value : values) {
                String val = value.trim();
                Identifier id;
                if (val.contains(":")) id = new Identifier(val);
                else id = new Identifier("minecraft", val);
                if (Registry.ENTITY_TYPE.containsId(id)) entities.add(Registry.ENTITY_TYPE.get(id));
            }
        } catch (Exception ignored) {}

        return entities;
    }

    @Override
    public void resetWidget() {

    }

    @Override
    protected boolean isValueValid(List<EntityType<?>> value) {
        return true;
    }

    // TODO
    @Override
    protected String generateUsage() {
        return "(highlight)entity type (default)(pig, minecraft:zombie, etc)";
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = saveGeneral();

        ListTag valueTag = new ListTag();
        for (EntityType<?> entityType : get()) {
            valueTag.add(StringTag.of(Registry.ENTITY_TYPE.getId(entityType).toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<EntityType<?>> fromTag(CompoundTag tag) {
        get().clear();

        ListTag valueTag = tag.getList("value", 8);
        for (Tag tagI : valueTag) {
            EntityType<?> type = Registry.ENTITY_TYPE.get(new Identifier(tagI.asString()));
            if (!onlyAttackable || EntityUtils.isAttackable(type)) get().add(type);
        }

        changed();
        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private List<EntityType<?>> defaultValue;
        private Consumer<List<EntityType<?>>> onChanged;
        private Consumer<Setting<List<EntityType<?>>>> onModuleActivated;
        private boolean onlyAttackable = false;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(List<EntityType<?>> defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<List<EntityType<?>>> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<List<EntityType<?>>>> onModuleActivated) {
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
