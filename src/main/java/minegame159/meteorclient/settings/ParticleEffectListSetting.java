/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.settings;

import minegame159.meteorclient.gui.screens.settings.ParticleEffectListSettingScreen;
import minegame159.meteorclient.gui.widgets.WButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ParticleEffectListSetting extends Setting<List<ParticleEffect>> {

    public ParticleEffectListSetting(String name, String description, List<ParticleEffect> defaultValue, Consumer<List<ParticleEffect>> onChanged, Consumer<Setting<List<ParticleEffect>>> onModuleActivated) {
        super(name, description, defaultValue, onChanged, onModuleActivated);

        value = new ArrayList<>(defaultValue);

        widget = new WButton("Select");
        ((WButton) widget).action = () -> MinecraftClient.getInstance().openScreen(new ParticleEffectListSettingScreen(this));
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
    protected List<ParticleEffect> parseImpl(String str) {
        String[] values = str.split(",");
        List<ParticleEffect> particleTypes = new ArrayList<>(1);

        try {
            for (String value : values) {
                String val = value.trim();
                Identifier id;
                if (val.contains(":")) id = new Identifier(val);
                else id = new Identifier("minecraft", val);
                ParticleType<?> particleType = Registry.PARTICLE_TYPE.get(id);

                if (Registry.PARTICLE_TYPE.containsId(id) && particleType instanceof ParticleEffect) particleTypes.add((ParticleEffect) particleType);
            }
        } catch (Exception ignored) {}

        return particleTypes;
    }

    @Override
    public void resetWidget() {

    }

    @Override
    protected boolean isValueValid(List<ParticleEffect> value) {
        return true;
    }

    @Override
    protected String generateUsage() {
        return "(highlight)particle id (default)(hearts, minecraft:clouds, etc)";
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = saveGeneral();

        ListTag valueTag = new ListTag();
        for (ParticleEffect particleType : get()) {
            valueTag.add(StringTag.of(Registry.PARTICLE_TYPE.getId((ParticleType<?>) particleType).toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<ParticleEffect> fromTag(CompoundTag tag) {
        get().clear();

        ListTag valueTag = tag.getList("value", 8);
        for (Tag tagI : valueTag) {
            get().add((ParticleEffect) Registry.PARTICLE_TYPE.get(new Identifier(tagI.asString())));
        }

        changed();
        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private List<ParticleEffect> defaultValue;
        private Consumer<List<ParticleEffect>> onChanged;
        private Consumer<Setting<List<ParticleEffect>>> onModuleActivated;

        public ParticleEffectListSetting.Builder name(String name) {
            this.name = name;
            return this;
        }

        public ParticleEffectListSetting.Builder description(String description) {
            this.description = description;
            return this;
        }

        public ParticleEffectListSetting.Builder defaultValue(List<ParticleEffect> defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public ParticleEffectListSetting.Builder onChanged(Consumer<List<ParticleEffect>> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public ParticleEffectListSetting.Builder onModuleActivated(Consumer<Setting<List<ParticleEffect>>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public ParticleEffectListSetting build() {
            return new ParticleEffectListSetting(name, description, defaultValue, onChanged, onModuleActivated);
        }
    }
}
