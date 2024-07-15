/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import meteordevelopment.meteorclient.utils.network.PacketUtils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class PacketListSetting extends Setting<Set<PacketType<? extends Packet<?>>>> {
    private final Predicate<PacketType<? extends Packet<?>>> filter;

    public PacketListSetting(String name, String description, Set<PacketType<? extends Packet<?>>> defaultValue, Consumer<Set<PacketType<? extends Packet<?>>>> onChanged, Consumer<Setting<Set<PacketType<? extends Packet<?>>>>> onModuleActivated, Predicate<PacketType<? extends Packet<?>>> filter, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.filter = filter;
    }

    public boolean filter(PacketType<? extends Packet<?>> packetType) {
        return filter == null || filter.test(packetType);
    }

    @Override
    public void resetImpl() {
        value = new ReferenceOpenHashSet<>(defaultValue);
    }

    @Override
    protected Set<PacketType<? extends Packet<?>>> parseImpl(String str) {
        String[] values = str.split(",");
        Set<PacketType<? extends Packet<?>>> packets = new ReferenceOpenHashSet<>(values.length);

        try {
            for (String value : values) {
                PacketType<? extends Packet<?>> packet = PacketUtils.getPacket(value.trim());
                if (packet != null && filter(packet)) packets.add(packet);
            }
        } catch (Exception ignored) {}

        return packets;
    }

    @Override
    protected boolean isValueValid(Set<PacketType<? extends Packet<?>>> value) {
        return true;
    }

    @Override
    public Iterable<String> getSuggestions() {
        return PacketUtils.getPacketEntries().stream().filter(entry -> this.filter(entry.getKey())).map(Map.Entry::getValue).toList();
    }

    @Override
    public NbtCompound save(NbtCompound tag) {
        NbtList valueTag = new NbtList();
        for (PacketType<? extends Packet<?>> packet : get()) {
            valueTag.add(NbtString.of(PacketUtils.getName(packet)));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public Set<PacketType<? extends Packet<?>>> load(NbtCompound tag) {
        get().clear();

        NbtElement valueTag = tag.get("value");
        if (valueTag instanceof NbtList) {
            for (NbtElement t : (NbtList) valueTag) {
                PacketType<? extends Packet<?>> packet = PacketUtils.getPacket(t.asString());
                if (packet != null && (filter == null || filter.test(packet))) get().add(packet);
            }
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, Set<PacketType<? extends Packet<?>>>, PacketListSetting> {
        private Predicate<PacketType<? extends Packet<?>>> filter;

        public Builder() {
            super(Set.of());
        }

        @SafeVarargs
        public final Builder defaultValue(PacketType<? extends Packet<?>>... defaults) {
            return defaultValue(new ReferenceOpenHashSet<>(Arrays.asList(defaults)));
        }

        public Builder filter(Predicate<PacketType<? extends Packet<?>>> filter) {
            this.filter = filter;
            return this;
        }

        public Builder clientboundOnly() {
            return filter(type -> type.side() == NetworkSide.CLIENTBOUND);
        }

        public Builder serverboundOnly() {
            return filter(type -> type.side() == NetworkSide.SERVERBOUND);
        }

        @Override
        public PacketListSetting build() {
            return new PacketListSetting(name, description, defaultValue, onChanged, onModuleActivated, filter, visible);
        }
    }
}
