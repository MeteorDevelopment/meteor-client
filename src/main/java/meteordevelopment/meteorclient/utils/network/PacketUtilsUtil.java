/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.network;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import net.minecraft.network.packet.BundlePacket;
import net.minecraft.network.packet.BundleSplitterPacket;
import net.minecraft.network.packet.Packet;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public final class PacketUtilsUtil {
    private PacketUtilsUtil() {
    }

    public static void main(String[] args) {
        try {
            init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void init() throws IOException {
        // Target path
        Path filePath = Path.of(
            "src", "main", "java",
            PacketUtilsUtil.class.getPackageName().replace('.', File.separatorChar),
            "PacketUtils.java"
        );

        Files.createDirectories(filePath.getParent());

        // Generate mappings
        String c2sMappings = processPackets("net.minecraft.network.packet.c2s", "C2S_PACKETS", "C2S_PACKETS_R",
            packet -> false
        );
        String s2cMappings = processPackets("net.minecraft.network.packet.s2c", "S2C_PACKETS", "S2C_PACKETS_R",
            packet -> BundlePacket.class.isAssignableFrom(packet) || BundleSplitterPacket.class.isAssignableFrom(packet)
        );

        // Write to file
        String content = """
            /*
             * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
             * Copyright (c) Meteor Development.
             */

            package meteordevelopment.meteorclient.utils.network;

            import com.google.common.collect.Sets;
            import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
            import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
            import net.minecraft.network.packet.Packet;

            import java.util.Map;
            import java.util.Set;

            public class PacketUtils {
                private static final Map<Class<? extends Packet<?>>, String> S2C_PACKETS = new Reference2ObjectOpenHashMap<>();
                private static final Map<Class<? extends Packet<?>>, String> C2S_PACKETS = new Reference2ObjectOpenHashMap<>();

                private static final Map<String, Class<? extends Packet<?>>> S2C_PACKETS_R = new Object2ReferenceOpenHashMap<>();
                private static final Map<String, Class<? extends Packet<?>>> C2S_PACKETS_R = new Object2ReferenceOpenHashMap<>();

                public static final Set<Class<? extends Packet<?>>> PACKETS = Sets.union(getC2SPackets(), getS2CPackets());

                static {
            %s

            %s
                }

                private PacketUtils() {
                }

                public static String getName(Class<? extends Packet<?>> packetClass) {
                    String name = S2C_PACKETS.get(packetClass);
                    if (name != null) return name;
                    return C2S_PACKETS.get(packetClass);
                }

                public static Class<? extends Packet<?>> getPacket(String name) {
                    Class<? extends Packet<?>> packet = S2C_PACKETS_R.get(name);
                    if (packet != null) return packet;
                    return C2S_PACKETS_R.get(name);
                }

                public static Set<Class<? extends Packet<?>>> getS2CPackets() {
                    return S2C_PACKETS.keySet();
                }

                public static Set<Class<? extends Packet<?>>> getC2SPackets() {
                    return C2S_PACKETS.keySet();
                }
            }
            """.formatted(
            c2sMappings.indent(8).stripTrailing(),
            s2cMappings.indent(8).stripTrailing()
        );

        Files.writeString(filePath, content, StandardCharsets.UTF_8);
    }

    private static String processPackets(String packageName, String packetMapName, String reverseMapName, Predicate<Class<?>> exclusionFilter) {
        Comparator<Class<?>> packetsComparator = Comparator
            .comparing((Class<?> cls) -> cls.getName().substring(cls.getName().lastIndexOf('.') + 1))
            .thenComparing(Class::getName);

        StringBuilder mappings = new StringBuilder(8192);

        try (ScanResult scanResult = new ClassGraph()
            .acceptPackages(packageName)
            .enableClassInfo()
            .ignoreClassVisibility()
            .scan()) {

            var packets = scanResult.getClassesImplementing(Packet.class).stream()
                .map(ClassInfo::loadClass)
                .filter(exclusionFilter.negate())
                .sorted(packetsComparator)
                .toList();

            for (Class<?> packet : packets) {
                String name = packet.getName();
                String className = name.substring(name.lastIndexOf('.') + 1).replace('$', '.');
                String fullName = name.replace('$', '.');

                mappings.append("%s.put(%s.class, \"%s\");%n".formatted(packetMapName, fullName, className));
                mappings.append("%s.put(\"%s\", %s.class);%n".formatted(reverseMapName, className, fullName));
            }
        }

        return mappings.toString();
    }
}
