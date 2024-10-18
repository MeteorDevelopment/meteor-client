/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.network;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.state.PlayStateFactories;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PacketUtils {
    public static final Registry<PacketType<? extends Packet<?>>> REGISTRY = new PacketRegistry();

    private static final Reference2ObjectOpenHashMap<PacketType<? extends Packet<?>>, String> PACKET_MAPPINGS = new Reference2ObjectOpenHashMap<>();
    private static final Object2ReferenceOpenHashMap<String, PacketType<? extends Packet<?>>> PACKET_MAPPINGS_R = new Object2ReferenceOpenHashMap<>();

    static {
        Map<NetworkSide, String> enumNameCache = new EnumMap<>(Arrays.stream(NetworkSide.values())
            .collect(Collectors.toMap(Function.identity(), side -> StringUtils.capitalize(side.getName()) + "/"))
        );

        PlayStateFactories.C2S.forEachPacketType((packetType, pid) -> {
            String name = toName(enumNameCache, packetType);

            PACKET_MAPPINGS.put(packetType, name);
            PACKET_MAPPINGS_R.put(name, packetType);
        });

        PlayStateFactories.S2C.forEachPacketType((packetType, pid) -> {
            String name = toName(enumNameCache, packetType);

            PACKET_MAPPINGS.put(packetType, name);
            PACKET_MAPPINGS_R.put(name, packetType);
        });

        PACKET_MAPPINGS.trim();
        PACKET_MAPPINGS_R.trim();
    }

    private static String toName(Map<NetworkSide, String> networkSideCache, PacketType<? extends Packet<?>> packetType) {
        StringBuilder nameBuilder = new StringBuilder();

        String namespace = packetType.id().getNamespace();
        if (!namespace.equals("minecraft")) nameBuilder.append(namespace).append(':');

        nameBuilder.append(networkSideCache.get(packetType.side()));

        for (String token : packetType.id().getPath().split("_")) {
            nameBuilder.append(StringUtils.capitalize(token));
        }

        return nameBuilder.toString();
    }

    private PacketUtils() {
    }

    public static String getName(PacketType<? extends Packet<?>> packetClass) {
        return PACKET_MAPPINGS.get(packetClass);
    }

    /**
     * When deserializing from user input/saved configs, you should explicitly handle {@code null} return values.
     */
    @Nullable
    public static PacketType<? extends Packet<?>> getPacket(String name) {
        if (name.startsWith("minecraft:")) name = name.substring(10);
        return PACKET_MAPPINGS_R.get(name);
    }

    public static Set<PacketType<? extends Packet<?>>> getPackets() {
        return PACKET_MAPPINGS.keySet();
    }

    public static Collection<String> getPacketNames() {
        return PACKET_MAPPINGS.values();
    }

    public static Set<Reference2ObjectMap.Entry<PacketType<? extends Packet<?>>, String>> getPacketEntries() {
        return PACKET_MAPPINGS.reference2ObjectEntrySet();
    }

    private static class PacketRegistry extends SimpleRegistry<PacketType<? extends Packet<?>>> {
        public PacketRegistry() {
            super(RegistryKey.ofRegistry(MeteorClient.identifier("packets")), Lifecycle.stable());
        }

        @Override
        public int size() {
            return PACKET_MAPPINGS.size();
        }

        @Override
        public Identifier getId(PacketType<? extends Packet<?>> entry) {
            return null;
        }

        @Override
        public Optional<RegistryKey<PacketType<? extends Packet<?>>>> getKey(PacketType<? extends Packet<?>> entry) {
            return Optional.empty();
        }

        @Override
        public int getRawId(PacketType<? extends Packet<?>> entry) {
            return 0;
        }

        @Override
        public PacketType<? extends Packet<?>> get(RegistryKey<PacketType<? extends Packet<?>>> key) {
            return null;
        }

        @Override
        public PacketType<? extends Packet<?>> get(Identifier id) {
            return null;
        }

        @Override
        public Lifecycle getLifecycle() {
            return null;
        }

        @Override
        public Set<Identifier> getIds() {
            return Collections.emptySet();
        }

        @Override
        public boolean containsId(Identifier id) {
            return false;
        }

        @Override
        public PacketType<? extends Packet<?>> get(int index) {
            return null;
        }

        @NotNull
        @Override
        public Iterator<PacketType<? extends Packet<?>>> iterator() {
            return PACKET_MAPPINGS.keySet().iterator();
        }

        @Override
        public boolean contains(RegistryKey<PacketType<? extends Packet<?>>> key) {
            return false;
        }

        @Override
        public Set<Map.Entry<RegistryKey<PacketType<? extends Packet<?>>>, PacketType<? extends Packet<?>>>> getEntrySet() {
            return Collections.emptySet();
        }

        @Override
        public Optional<RegistryEntry.Reference<PacketType<? extends Packet<?>>>> getRandom(Random random) {
            return Optional.empty();
        }

        @Override
        public Registry<PacketType<? extends Packet<?>>> freeze() {
            return null;
        }

        @Override
        public RegistryEntry.Reference<PacketType<? extends Packet<?>>> createEntry(PacketType<? extends Packet<?>> value) {
            return null;
        }

        @Override
        public Optional<RegistryEntry.Reference<PacketType<? extends Packet<?>>>> getEntry(int rawId) {
            return Optional.empty();
        }

        @Override
        public Optional<RegistryEntry.Reference<PacketType<? extends Packet<?>>>> getEntry(RegistryKey<PacketType<? extends Packet<?>>> key) {
            return Optional.empty();
        }

        @Override
        public Stream<RegistryEntry.Reference<PacketType<? extends Packet<?>>>> streamEntries() {
            return null;
        }

        @Override
        public Optional<RegistryEntryList.Named<PacketType<? extends Packet<?>>>> getEntryList(TagKey<PacketType<? extends Packet<?>>> tag) {
            return Optional.empty();
        }

        @Override
        public RegistryEntryList.Named<PacketType<? extends Packet<?>>> getOrCreateEntryList(TagKey<PacketType<? extends Packet<?>>> tag) {
            return null;
        }

        @Override
        public Stream<Pair<TagKey<PacketType<? extends Packet<?>>>, RegistryEntryList.Named<PacketType<? extends Packet<?>>>>> streamTagsAndEntries() {
            return null;
        }

        @Override
        public Stream<TagKey<PacketType<? extends Packet<?>>>> streamTags() {
            return null;
        }

        @Override
        public void clearTags() {}

        @Override
        public void populateTags(Map<TagKey<PacketType<? extends Packet<?>>>, List<RegistryEntry<PacketType<? extends Packet<?>>>>> tagEntries) {}

        @Override
        public Set<RegistryKey<PacketType<? extends Packet<?>>>> getKeys() {
            return Collections.emptySet();
        }
    }
}
