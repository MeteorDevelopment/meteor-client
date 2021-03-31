/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.settings;

import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import minegame159.meteorclient.utils.network.PacketUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PacketBoolSetting extends Setting<Object2BooleanMap<Class<? extends Packet<?>>>> {
    private static List<String> suggestions;

    public PacketBoolSetting(String name, String description, Object2BooleanMap<Class<? extends Packet<?>>> defaultValue, Consumer<Object2BooleanMap<Class<? extends Packet<?>>>> onChanged, Consumer<Setting<Object2BooleanMap<Class<? extends Packet<?>>>>> onModuleActivated) {
        super(name, description, defaultValue, onChanged, onModuleActivated);

        value = new Object2BooleanArrayMap<>(defaultValue);
    }

    @Override
    public void reset(boolean callbacks) {
        value = new Object2BooleanArrayMap<>(defaultValue);
        if (callbacks) changed();
    }

    @Override
    protected Object2BooleanMap<Class<? extends Packet<?>>> parseImpl(String str) {
        String[] values = str.split(",");
        Object2BooleanMap<Class<? extends Packet<?>>> packets = new Object2BooleanOpenHashMap<>(values.length);

        try {
            for (String value : values) {
                Class<? extends Packet<?>> packet = PacketUtils.getPacket(value.trim());
                if (packet != null) packets.put(packet, true);
            }
        } catch (Exception ignored) {}

        return packets;
    }

    @Override
    protected boolean isValueValid(Object2BooleanMap<Class<? extends Packet<?>>> value) {
        return true;
    }

    @Override
    public List<String> getSuggestions() {
        if (suggestions == null) {
            suggestions = new ArrayList<>(PacketUtils.getC2SPackets().size() + PacketUtils.getS2CPackets().size());

            for (Class<? extends Packet<?>> packet : PacketUtils.getC2SPackets()) {
                suggestions.add(PacketUtils.getName(packet));
            }

            for (Class<? extends Packet<?>> packet : PacketUtils.getS2CPackets()) {
                suggestions.add(PacketUtils.getName(packet));
            }
        }

        return suggestions;
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
