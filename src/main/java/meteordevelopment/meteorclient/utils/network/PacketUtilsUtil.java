/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.network;

import net.minecraft.network.Packet;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

public class PacketUtilsUtil {
    private static final String packetRegistryClass = """
    private static class PacketRegistry extends Registry<Class<? extends Packet<?>>> {
        public PacketRegistry() {
            super(RegistryKey.ofRegistry(new MeteorIdentifier("packets")), Lifecycle.stable());
        }

        @Override
        public int size() {
            return S2C_PACKETS.keySet().size() + C2S_PACKETS.keySet().size();
        }

        @Override
        public Identifier getId(Class<? extends Packet<?>> entry) {
            return null;
        }

        @Override
        public Optional<RegistryKey<Class<? extends Packet<?>>>> getKey(Class<? extends Packet<?>> entry) {
            return Optional.empty();
        }

        @Override
        public int getRawId(Class<? extends Packet<?>> entry) {
            return 0;
        }

        @Override
        public Class<? extends Packet<?>> get(RegistryKey<Class<? extends Packet<?>>> key) {
            return null;
        }

        @Override
        public Class<? extends Packet<?>> get(Identifier id) {
            return null;
        }

        @Override
        public Lifecycle getEntryLifecycle(Class<? extends Packet<?>> object) {
            return null;
        }

        @Override
        public Lifecycle getLifecycle() {
            return null;
        }

        @Override
        public Set<Identifier> getIds() {
            return null;
        }

        @Override
        public boolean containsId(Identifier id) {
            return false;
        }

        @Override
        public Class<? extends Packet<?>> get(int index) {
            return null;
        }

        @NotNull
        @Override
        public Iterator<Class<? extends Packet<?>>> iterator() {
            return Iterators.concat(S2C_PACKETS.keySet().iterator(), C2S_PACKETS.keySet().iterator());
        }

        @Override
        public boolean contains(RegistryKey<Class<? extends Packet<?>>> key) {
            return false;
        }

        @Override
        public Set<Map.Entry<RegistryKey<Class<? extends Packet<?>>>, Class<? extends Packet<?>>>> getEntrySet() {
            return null;
        }

        @Override
        public Optional<RegistryEntry<Class<? extends Packet<?>>>> getRandom(Random random) {
            return Optional.empty();
        }

        @Override
        public Registry<Class<? extends Packet<?>>> freeze() {
            return null;
        }

        @Override
        public RegistryEntry<Class<? extends Packet<?>>> getOrCreateEntry(RegistryKey<Class<? extends Packet<?>>> key) {
            return null;
        }

        @Override
        public RegistryEntry.Reference<Class<? extends Packet<?>>> createEntry(Class<? extends Packet<?>> value) {
            return null;
        }

        @Override
        public Optional<RegistryEntry<Class<? extends Packet<?>>>> getEntry(int rawId) {
            return Optional.empty();
        }

        @Override
        public Optional<RegistryEntry<Class<? extends Packet<?>>>> getEntry(RegistryKey<Class<? extends Packet<?>>> key) {
            return Optional.empty();
        }

        @Override
        public Stream<RegistryEntry.Reference<Class<? extends Packet<?>>>> streamEntries() {
            return null;
        }

        @Override
        public Optional<RegistryEntryList.Named<Class<? extends Packet<?>>>> getEntryList(TagKey<Class<? extends Packet<?>>> tag) {
            return Optional.empty();
        }

        @Override
        public RegistryEntryList.Named<Class<? extends Packet<?>>> getOrCreateEntryList(TagKey<Class<? extends Packet<?>>> tag) {
            return null;
        }

        @Override
        public Stream<Pair<TagKey<Class<? extends Packet<?>>>, RegistryEntryList.Named<Class<? extends Packet<?>>>>> streamTagsAndEntries() {
            return null;
        }

        @Override
        public Stream<TagKey<Class<? extends Packet<?>>>> streamTags() {
            return null;
        }

        @Override
        public boolean containsTag(TagKey<Class<? extends Packet<?>>> tag) {
            return false;
        }

        @Override
        public void clearTags() {}

        @Override
        public void populateTags(Map<TagKey<Class<? extends Packet<?>>>, List<RegistryEntry<Class<? extends Packet<?>>>>> tagEntries) {}

        @Override
        public DataResult<RegistryEntry<Class<? extends Packet<?>>>> getOrCreateEntryDataResult(RegistryKey<Class<? extends Packet<?>>> key) {
            return null;
        }

        @Override
        public Set<RegistryKey<Class<? extends Packet<?>>>> getKeys() {
            return null;
        }
    }
""";

//    @PostInit
    public static void init() throws IOException {
        // Generate PacketUtils.java
        File file = new File(System.getProperty("user.dir") + "/PacketUtils.java");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        writer.write("/*\n");
        writer.write(" * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).\n");
        writer.write(" * Copyright (c) Meteor Development.\n");
        writer.write(" */\n\n");

        writer.write("package meteordevelopment.meteorclient.utils.network;\n\n");

        //   Write imports
        writer.write("import com.google.common.collect.Iterators;\n");
        writer.write("import com.mojang.datafixers.util.Pair;\n");
        writer.write("import com.mojang.serialization.Lifecycle;\n");
        writer.write("import meteordevelopment.meteorclient.utils.misc.MeteorIdentifier;\n");
        writer.write("import net.minecraft.network.Packet;\n");
        writer.write("import net.minecraft.tag.TagKey;\n");
        writer.write("import net.minecraft.util.Identifier;\n");
        writer.write("import net.minecraft.util.registry.Registry;\n");
        writer.write("import net.minecraft.util.registry.RegistryEntry;\n");
        writer.write("import net.minecraft.util.registry.RegistryEntryList;\n");
        writer.write("import net.minecraft.util.registry.RegistryKey;\n");
        writer.write("import org.jetbrains.annotations.NotNull;\n");
        writer.write("import net.minecraft.util.math.random.Random;\n");
        writer.write("import com.mojang.serialization.DataResult;\n");
        writer.write("import java.util.*;\n");
        writer.write("import java.util.stream.Stream;\n");

        //   Write class
        writer.write("\npublic class PacketUtils {\n");

        //     Write fields
        writer.write("    public static final Registry<Class<? extends Packet<?>>> REGISTRY = new PacketRegistry();\n\n");
        writer.write("    private static final Map<Class<? extends Packet<?>>, String> S2C_PACKETS = new HashMap<>();\n");
        writer.write("    private static final Map<Class<? extends Packet<?>>, String> C2S_PACKETS = new HashMap<>();\n\n");
        writer.write("    private static final Map<String, Class<? extends Packet<?>>> S2C_PACKETS_R = new HashMap<>();\n");
        writer.write("    private static final Map<String, Class<? extends Packet<?>>> C2S_PACKETS_R = new HashMap<>();\n\n");

        //     Write static block
        writer.write("    static {\n");

        // Client -> Sever Packets
        Reflections c2s = new Reflections("net.minecraft.network.packet.c2s", Scanners.SubTypes);
        Set<Class<? extends Packet>> c2sPackets = c2s.getSubTypesOf(Packet.class);

        for (Class<? extends Packet> c2sPacket : c2sPackets) {
            String name = c2sPacket.getName();
            String className = name.substring(name.lastIndexOf('.') + 1).replace('$', '.');
            String fullName = name.replace('$', '.');

            writer.write(String.format("        C2S_PACKETS.put(%s.class, \"%s\");\n", fullName, className));
            writer.write(String.format("        C2S_PACKETS_R.put(\"%s\", %s.class);\n", className, fullName));
        }

        writer.newLine();

        // Server -> Client Packets
        Reflections s2c = new Reflections("net.minecraft.network.packet.s2c", Scanners.SubTypes);
        Set<Class<? extends Packet>> s2cPackets = s2c.getSubTypesOf(Packet.class);

        for (Class<? extends Packet> s2cPacket : s2cPackets) {
            String name = s2cPacket.getName();
            String className = name.substring(name.lastIndexOf('.') + 1).replace('$', '.');
            String fullName = name.replace('$', '.');

            writer.write(String.format("        S2C_PACKETS.put(%s.class, \"%s\");\n", fullName, className));
            writer.write(String.format("        S2C_PACKETS_R.put(\"%s\", %s.class);\n", className, fullName));
        }

        writer.write("    }\n\n");

        //     Write getName method
        writer.write("    public static String getName(Class<? extends Packet<?>> packetClass) {\n");
        writer.write("        String name = S2C_PACKETS.get(packetClass);\n");
        writer.write("        if (name != null) return name;\n");
        writer.write("        return C2S_PACKETS.get(packetClass);\n");
        writer.write("    }\n\n");

        //     Write getPacket method
        writer.write("    public static Class<? extends Packet<?>> getPacket(String name) {\n");
        writer.write("        Class<? extends Packet<?>> packet = S2C_PACKETS_R.get(name);\n");
        writer.write("        if (packet != null) return packet;\n");
        writer.write("        return C2S_PACKETS_R.get(name);\n");
        writer.write("    }\n\n");

        //     Write getS2CPackets method
        writer.write("    public static Set<Class<? extends Packet<?>>> getS2CPackets() {\n");
        writer.write("        return S2C_PACKETS.keySet();\n");
        writer.write("    }\n\n");

        //     Write getC2SPackets method
        writer.write("    public static Set<Class<? extends Packet<?>>> getC2SPackets() {\n");
        writer.write("        return C2S_PACKETS.keySet();\n");
        writer.write("    }\n\n");

        // Write PacketRegistry class
        writer.write(packetRegistryClass);

        //   Write end class
        writer.write("}\n");

        writer.close();
    }
}
