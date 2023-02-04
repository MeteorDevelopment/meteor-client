/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import meteordevelopment.meteorclient.utils.network.PacketUtils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.Packet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class PacketListSetting extends Setting<Set<Class<? extends Packet<?>>>> {
    public final Predicate<Class<? extends Packet<?>>> filter;
    private static List<String> suggestions;

    public PacketListSetting(String name, String description, Set<Class<? extends Packet<?>>> defaultValue, Consumer<Set<Class<? extends Packet<?>>>> onChanged, Consumer<Setting<Set<Class<? extends Packet<?>>>>> onModuleActivated, Predicate<Class<? extends Packet<?>>> filter, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.filter = filter;
    }

    @Override
    public void resetImpl() {
        value = new ObjectOpenHashSet<>(defaultValue);
    }

    @Override
    protected Set<Class<? extends Packet<?>>> parseImpl(String str) {
        String[] values = str.split(",");
        Set<Class<? extends Packet<?>>> packets = new ObjectOpenHashSet<>(values.length);

        try {
            for (String value : values) {
                Class<? extends Packet<?>> packet = PacketUtils.getPacket(value.trim());
                if (packet != null && (filter == null || filter.test(packet))) packets.add(packet);
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
    public NbtCompound save(NbtCompound tag) {
        NbtList valueTag = new NbtList();
        for (Class<? extends Packet<?>> packet : get()) {
            valueTag.add(NbtString.of(PacketUtils.getName(packet)));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public Set<Class<? extends Packet<?>>> load(NbtCompound tag) {
        get().clear();

        NbtElement valueTag = tag.get("value");
        if (valueTag instanceof NbtList) {
            for (NbtElement t : (NbtList) valueTag) {
                Class<? extends Packet<?>> packet = PacketUtils.getPacket(t.asString());
                if (packet != null && (filter == null || filter.test(packet))) get().add(packet);
            }
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, Set<Class<? extends Packet<?>>>, PacketListSetting> {
        private Predicate<Class<? extends Packet<?>>> filter;

        public Builder() {
            super(new ObjectOpenHashSet<>(0));
        }

        public Builder filter(Predicate<Class<? extends Packet<?>>> filter) {
            this.filter = filter;
            return this;
        }

        @Override
        public PacketListSetting build() {
            return new PacketListSetting(name, description, defaultValue, onChanged, onModuleActivated, filter, visible);
        }
    }
}
