/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.settings;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import minegame159.meteorclient.gui.screens.settings.StatusEffectSettingScreen;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.function.Consumer;

public class StatusEffectSetting extends Setting<Object2IntMap<StatusEffect>> {
    public StatusEffectSetting(String name, String description, Object2IntMap<StatusEffect> defaultValue, Consumer<Object2IntMap<StatusEffect>> onChanged, Consumer<Setting<Object2IntMap<StatusEffect>>> onModuleActivated) {
        super(name, description, defaultValue, onChanged, onModuleActivated);

        widget = new WButton("Select");
        ((WButton) widget).action = () -> MinecraftClient.getInstance().openScreen(new StatusEffectSettingScreen(this));
    }

    @Override
    public void reset(boolean callbacks) {
        value = new Object2IntArrayMap<>(defaultValue);
        if (callbacks) {
            resetWidget();
            changed();
        }
    }

    @Override
    protected Object2IntMap<StatusEffect> parseImpl(String str) {
        // TODO: I know this is wrong but im too lazy and nobody is going to use chat commands for packet canceller anyway
        return Utils.createStatusEffectMap();
    }

    @Override
    public void resetWidget() {

    }

    @Override
    protected boolean isValueValid(Object2IntMap<StatusEffect> value) {
        return true;
    }

    @Override
    protected String generateUsage() {
        //TODO: Look up retard
        return "(highlight)not implemented";
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = saveGeneral();

        CompoundTag valueTag = new CompoundTag();
        for (StatusEffect statusEffect : get().keySet()) {
            Identifier id = Registry.STATUS_EFFECT.getId(statusEffect);
            if (id != null) valueTag.putInt(id.toString(), get().getInt(statusEffect));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public Object2IntMap<StatusEffect> fromTag(CompoundTag tag) {
        get().clear();

        CompoundTag valueTag = tag.getCompound("value");
        for (String key : valueTag.getKeys()) {
            StatusEffect statusEffect = Registry.STATUS_EFFECT.get(new Identifier(key));
            if (statusEffect != null) get().put(statusEffect, valueTag.getInt(key));
        }

        changed();
        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private Object2IntMap<StatusEffect> defaultValue;
        private Consumer<Object2IntMap<StatusEffect>> onChanged;
        private Consumer<Setting<Object2IntMap<StatusEffect>>> onModuleActivated;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(Object2IntMap<StatusEffect> defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<Object2IntMap<StatusEffect>> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<Object2IntMap<StatusEffect>>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public StatusEffectSetting build() {
            return new StatusEffectSetting(name, description, defaultValue, onChanged, onModuleActivated);
        }
    }
}
