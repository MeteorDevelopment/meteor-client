/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import meteordevelopment.meteorclient.utils.network.PacketUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class PacketListSetting extends Setting<Set<PacketType<? extends @NotNull Packet<?>>>> {
    public final Predicate<PacketType<? extends @NotNull Packet<?>>> filter;
    private static List<String> suggestions;

    public PacketListSetting(String name, String description, Set<PacketType<? extends @NotNull Packet<?>>> defaultValue, Consumer<Set<PacketType<? extends @NotNull Packet<?>>>> onChanged, Consumer<Setting<Set<PacketType<? extends @NotNull Packet<?>>>>> onModuleActivated, Predicate<PacketType<? extends @NotNull Packet<?>>> filter, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.filter = filter;
    }

    @Override
    public void resetImpl() {
        value = new ObjectOpenHashSet<>(defaultValue);
    }

    @Override
    protected Set<PacketType<? extends @NotNull Packet<?>>> parseImpl(String str) {
        String[] values = str.split(",");
        Set<PacketType<? extends @NotNull Packet<?>>> packets = new ObjectOpenHashSet<>(values.length);

        try {
            for (String value : values) {
                PacketType<? extends @NotNull Packet<?>> packet = PacketUtils.getPacket(value.trim());
                if (packet != null && (filter == null || filter.test(packet))) packets.add(packet);
            }
        } catch (Exception _) {
        }

        return packets;
    }

    @Override
    protected boolean isValueValid(Set<PacketType<? extends @NotNull Packet<?>>> value) {
        return true;
    }

    @Override
    public List<String> getSuggestions() {
        if (suggestions == null) {
            suggestions = PacketUtils.getPackets().stream().map(PacketType::toString).toList();
        }

        return suggestions;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag valueTag = new ListTag();
        for (PacketType<? extends @NotNull Packet<?>> packet : get()) {
            valueTag.add(StringTag.valueOf(packet.toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public Set<PacketType<? extends @NotNull Packet<?>>> load(CompoundTag tag) {
        get().clear();

        Tag valueTag = tag.get("value");
        if (valueTag instanceof ListTag listTag) {
            for (Tag t : listTag) {
                PacketType<? extends @NotNull Packet<?>> packet = PacketUtils.getPacket(t.asString().orElse(""));
                if (packet != null && (filter == null || filter.test(packet))) get().add(packet);
            }
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, Set<PacketType<? extends @NotNull Packet<?>>>, PacketListSetting> {
        private Predicate<PacketType<? extends @NotNull Packet<?>>> filter;

        public Builder() {
            super(new ObjectOpenHashSet<>(0));
        }

        public Builder filter(Predicate<PacketType<? extends @NotNull Packet<?>>> filter) {
            this.filter = filter;
            return this;
        }

        public Builder clientbound() {
            return filter(type -> type.flow() == PacketFlow.CLIENTBOUND);
        }

        public Builder serverbound() {
            return filter(type -> type.flow() == PacketFlow.SERVERBOUND);
        }

        @Override
        public PacketListSetting build() {
            return new PacketListSetting(name, description, defaultValue, onChanged, onModuleActivated, filter, visible);
        }
    }
}
