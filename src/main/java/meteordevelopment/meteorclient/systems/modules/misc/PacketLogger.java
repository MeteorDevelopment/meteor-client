/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.PacketUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.packet.Packet;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@NullMarked
public class PacketLogger extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgOutput = settings.createGroup("Output");

    private final Setting<Set<Class<? extends Packet<?>>>> s2cPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("S2C-packets")
        .description("Server-to-client packets to log.")
        .filter(aClass -> PacketUtils.getS2CPackets().contains(aClass))
        .build()
    );

    private final Setting<Set<Class<? extends Packet<?>>>> c2sPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("C2S-packets")
        .description("Client-to-server packets to log.")
        .filter(aClass -> PacketUtils.getC2SPackets().contains(aClass))
        .build()
    );

    private final Setting<Boolean> showTimestamp = sgOutput.add(new BoolSetting.Builder()
        .name("show-timestamp")
        .description("Show timestamp for each logged packet.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showPacketData = sgOutput.add(new BoolSetting.Builder()
        .name("show-packet-data")
        .description("Show the packet's toString() data for debugging.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> showCount = sgOutput.add(new BoolSetting.Builder()
        .name("show-count")
        .description("Show how many times each packet type has been logged.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> logToFile = sgOutput.add(new BoolSetting.Builder()
        .name("log-to-file")
        .description("Save packet logs to a file in the meteor-client folder.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> flushInterval = sgOutput.add(new IntSetting.Builder()
        .name("flush-interval")
        .description("How often to flush logs to disk (in seconds).")
        .defaultValue(1)
        .min(1)
        .sliderMax(10)
        .visible(logToFile::get)
        .build()
    );

    private final Setting<Boolean> showSummary = sgOutput.add(new BoolSetting.Builder()
        .name("show-summary")
        .description("Show final packet count summary when module is deactivated.")
        .defaultValue(true)
        .build()
    );

    private final Object2IntOpenHashMap<Class<? extends Packet<?>>> packetCounts = new Object2IntOpenHashMap<>();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private final DateTimeFormatter fileNameFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private @Nullable BufferedWriter fileWriter;
    private long lastFlushMs;

    public PacketLogger() {
        super(Categories.Misc, "packet-logger", "Allows you to log certain packets.");
        runInMainMenu = true;
    }

    @Override
    public void onActivate() {
        if (fileWriter != null) {
            try {
                fileWriter.close();
            } catch (IOException e) {
                error("Failed to close previous log file: %s", e.getMessage());
            }
            fileWriter = null;
        }

        packetCounts.clear();
        lastFlushMs = System.currentTimeMillis();

        if (logToFile.get()) {
            try {
                Path logPath = MeteorClient.FOLDER.toPath().resolve("packet-logs");
                Files.createDirectories(logPath);

                String fileName = "packets-%s.log".formatted(LocalDateTime.now().format(fileNameFormatter));
                fileWriter = Files.newBufferedWriter(logPath.resolve(fileName), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                info("Logging packets to file: %s", fileName);
            } catch (IOException e) {
                error("Failed to create packet log file: %s", e.getMessage());
                fileWriter = null;
                lastFlushMs = 0;
            }
        }
    }

    @Override
    public void onDeactivate() {
        if (fileWriter != null) {
            try {
                fileWriter.flush();
                fileWriter.close();
                info("Closed packet log file.");
            } catch (IOException e) {
                error("Failed to close packet log file: %s", e.getMessage());
            }
            fileWriter = null;
        }

        if (showSummary.get() && !packetCounts.isEmpty()) {
            info("Final packet counts:");
            packetCounts.object2IntEntrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getIntValue(), a.getIntValue()))
                .forEach(e -> info("  %s: %d", PacketUtils.getName(e.getKey()), e.getIntValue()));
        }
    }

    private void logPacket(String direction, Packet<?> packet) {
        @SuppressWarnings("unchecked")
        Class<? extends Packet<?>> packetClass = (Class<? extends Packet<?>>) packet.getClass();

        // Update count
        packetCounts.addTo(packetClass, 1);

        // Build log message
        StringBuilder msg = new StringBuilder(128);

        if (showTimestamp.get()) {
            msg.append("[").append(LocalDateTime.now().format(timeFormatter)).append("] ");
        }

        msg.append(direction).append(" ");
        msg.append(PacketUtils.getName(packetClass));

        if (showCount.get()) {
            msg.append(" (#").append(packetCounts.getInt(packetClass)).append(")");
        }

        if (showPacketData.get()) {
            msg.append("\n  Data: ").append(packet);
        }

        // Log to chat
        info(msg.toString());

        // Log to file
        if (logToFile.get() && fileWriter != null) {
            try {
                fileWriter.write(msg.toString());
                fileWriter.newLine();

                // Flush periodically, not on every packet
                long now = System.currentTimeMillis();
                long flushIntervalMs = flushInterval.get() * 1000L;
                if (now - lastFlushMs >= flushIntervalMs) {
                    fileWriter.flush();
                    lastFlushMs = now;
                }
            } catch (IOException e) {
                error("Failed to write to packet log file: %s. File logging disabled.", e.getMessage());
                try {
                    fileWriter.close();
                } catch (IOException ignored) {
                    // Close attempt after error
                }
                fileWriter = null;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onReceivePacket(PacketEvent.Receive event) {
        if (s2cPackets.get().contains(event.packet.getClass())) {
            logPacket("<- S2C", event.packet);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onSendPacket(PacketEvent.Send event) {
        if (c2sPackets.get().contains(event.packet.getClass())) {
            logPacket("-> C2S", event.packet);
        }
    }
}
