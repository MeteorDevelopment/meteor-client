/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.settings;

import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import minegame159.meteorclient.gui.screens.settings.PacketBoolSettingScreen;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.utils.network.PacketUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;

import java.util.function.Consumer;

public class PacketBoolSetting extends Setting<Object2BooleanMap<Class<? extends Packet<?>>>> {
    public PacketBoolSetting(String name, String description, Object2BooleanMap<Class<? extends Packet<?>>> defaultValue, Consumer<Object2BooleanMap<Class<? extends Packet<?>>>> onChanged, Consumer<Setting<Object2BooleanMap<Class<? extends Packet<?>>>>> onModuleActivated) {
        super(name, description, defaultValue, onChanged, onModuleActivated);

        value = new Object2BooleanArrayMap<>(defaultValue);

        widget = new WButton("Select");
        ((WButton) widget).action = () -> MinecraftClient.getInstance().openScreen(new PacketBoolSettingScreen(this));
    }

    @Override
    public void reset(boolean callbacks) {
        value = new Object2BooleanArrayMap<>(defaultValue);
        if (callbacks) {
            resetWidget();
            changed();
        }
    }

    @Override
    protected Object2BooleanMap<Class<? extends Packet<?>>> parseImpl(String str) {
        // TODO: I know this is wrong but im too lazy and nobody is going to use chat commands for packet canceller anyway
        return new Object2BooleanArrayMap<>();
    }

    @Override
    public void resetWidget() {

    }

    @Override
    protected boolean isValueValid(Object2BooleanMap<Class<? extends Packet<?>>> value) {
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
        for (Class<? extends Packet<?>> packet : get().keySet()) {
            valueTag.putBoolean(PacketUtils.getName(packet), get().getBoolean(packet));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public Object2BooleanMap<Class<? extends Packet<?>>> fromTag(CompoundTag tag) {
        get().clear();

        CompoundTag valueTag = tag.getCompound("value");
        for (String key : valueTag.getKeys()) {
            Class<? extends Packet<?>> packet = PacketUtils.getPacket(key);
            if (packet != null) get().put(packet, valueTag.getBoolean(key));
        }

        changed();
        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private Object2BooleanMap<Class<? extends Packet<?>>> defaultValue;
        private Consumer<Object2BooleanMap<Class<? extends Packet<?>>>> onChanged;
        private Consumer<Setting<Object2BooleanMap<Class<? extends Packet<?>>>>> onModuleActivated;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(Object2BooleanMap<Class<? extends Packet<?>>> defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<Object2BooleanMap<Class<? extends Packet<?>>>> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<Object2BooleanMap<Class<? extends Packet<?>>>>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public PacketBoolSetting build() {
            return new PacketBoolSetting(name, description, defaultValue, onChanged, onModuleActivated);
        }
    }
}
