/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.settings;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import minegame159.meteorclient.utils.network.PacketUtils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.Packet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class PacketBoolSetting extends Setting<Set<Class<? extends Packet<?>>>> {
    private static List<String> suggestions;

    public PacketBoolSetting(String name, String description, Set<Class<? extends Packet<?>>> defaultValue, Consumer<Set<Class<? extends Packet<?>>>> onChanged, Consumer<Setting<Set<Class<? extends Packet<?>>>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        value = new ObjectOpenHashSet<>(defaultValue);
    }

    @Override
    public void reset(boolean callbacks) {
        value = new ObjectOpenHashSet<>(defaultValue);
        if (callbacks) changed();
    }

    @Override
    protected Set<Class<? extends Packet<?>>> parseImpl(String str) {
        String[] values = str.split(",");
        Set<Class<? extends Packet<?>>> packets = new ObjectOpenHashSet<>(values.length);

        try {
            for (String value : values) {
                Class<? extends Packet<?>> packet = PacketUtils.getPacket(value.trim());
                if (packet != null) packets.add(packet);
            }
        } catch (Exception ignored) {}

        return packets;
    }

    @Override
    protected boolean isValueValid(Set<Class<? extends Packet<?>>> value) {
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
    public NbtCompound toTag() {
        NbtCompound tag = saveGeneral();

        NbtList valueTag = new NbtList();
        for (Class<? extends Packet<?>> packet : get()) {
            valueTag.add(NbtString.of(PacketUtils.getName(packet)));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public Set<Class<? extends Packet<?>>> fromTag(NbtCompound tag) {
        get().clear();

        NbtElement valueTag = tag.get("value");
        if (valueTag instanceof NbtList) {
            for (NbtElement t : (NbtList) valueTag) {
                Class<? extends Packet<?>> packet = PacketUtils.getPacket(t.asString());
                if (packet != null) get().add(packet);
            }
        }

        changed();
        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private Set<Class<? extends Packet<?>>> defaultValue;
        private Consumer<Set<Class<? extends Packet<?>>>> onChanged;
        private Consumer<Setting<Set<Class<? extends Packet<?>>>>> onModuleActivated;
        private IVisible visible;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(Set<Class<? extends Packet<?>>> defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<Set<Class<? extends Packet<?>>>> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<Set<Class<? extends Packet<?>>>>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public Builder visible(IVisible visible) {
            this.visible = visible;
            return this;
        }

        public PacketBoolSetting build() {
            return new PacketBoolSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
