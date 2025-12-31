/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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

    private final Setting<Boolean> showSummary = sgOutput.add(new BoolSetting.Builder()
        .name("show-summary")
        .description("Show final packet count summary when module is deactivated.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> logToChat = sgOutput.add(new BoolSetting.Builder()
        .name("log-to-chat")
        .description("Log packets to chat.")
        .defaultValue(true)
        .build()
    );

    // File logging settings

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

    private final Setting<Integer> maxFileSizeMB = sgOutput.add(new IntSetting.Builder()
        .name("max-file-size-mb")
        .description("Maximum size per log file in MB. Creates new file when exceeded.")
        .defaultValue(10)
        .min(1)
        .sliderMax(100)
        .visible(logToFile::get)
        .build()
    );

    private final Setting<Integer> maxTotalLogsMB = sgOutput.add(new IntSetting.Builder()
        .name("max-total-logs-mb")
        .description("Maximum total disk space for all packet logs in MB. Deletes oldest when exceeded.")
        .defaultValue(50)
        .min(1)
        .sliderMax(500)
        .visible(logToFile::get)
        .build()
    );

    private static final Path PACKET_LOGS_DIR = MeteorClient.FOLDER.toPath().resolve("packet-logs");
    private static final Charset LOG_CHARSET = StandardCharsets.UTF_8;
    private static final int LINE_SEPARATOR_BYTES = System.lineSeparator().getBytes(LOG_CHARSET).length;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private final Reference2IntOpenHashMap<Class<? extends Packet<?>>> packetCounts = new Reference2IntOpenHashMap<>();
    private @Nullable BufferedWriter fileWriter;
    private long lastFlushMs;
    private long currentFileSizeBytes;
    private int currentFileIndex;
    private @Nullable LocalDateTime sessionStartTime;

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
        sessionStartTime = LocalDateTime.now();
        currentFileIndex = 0;
        currentFileSizeBytes = 0;

        if (logToFile.get()) {
            try {
                Files.createDirectories(PACKET_LOGS_DIR);
                cleanupOldLogs();

                String fileName = "packets-%s-%d.log".formatted(
                    sessionStartTime.format(FILE_NAME_FORMATTER),
                    currentFileIndex
                );
                fileWriter = Files.newBufferedWriter(PACKET_LOGS_DIR.resolve(fileName), LOG_CHARSET, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
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
            } catch (IOException e) {
                error("Failed to close packet log file: %s", e.getMessage());
            }
            fileWriter = null;
        }

        if (showSummary.get() && !packetCounts.isEmpty()) {
            int totalPackets = packetCounts.values().intStream().sum();
            info("Final packet counts (total %d):", totalPackets);
            packetCounts.reference2IntEntrySet().stream()
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
            msg.append("[").append(LocalDateTime.now().format(TIME_FORMATTER)).append("] ");
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
        if (logToChat.get()) info(msg.toString());

        // Log to file
        if (logToFile.get() && fileWriter != null) {
            try {
                String line = msg.toString();
                int lineBytes = line.getBytes(LOG_CHARSET).length + LINE_SEPARATOR_BYTES;

                if (currentFileSizeBytes + lineBytes > maxFileSizeMB.get() * 1024L * 1024L) {
                    rotateLogFile();
                }

                fileWriter.write(line);
                fileWriter.newLine();
                currentFileSizeBytes += lineBytes;

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

    /**
     * Rotates the current log file when it exceeds the maximum size.
     */
    private void rotateLogFile() throws IOException {
        fileWriter.flush();
        fileWriter.close();

        currentFileIndex++;

        String fileName = "packets-%s-%d.log".formatted(
            sessionStartTime.format(FILE_NAME_FORMATTER),
            currentFileIndex
        );

        fileWriter = Files.newBufferedWriter(PACKET_LOGS_DIR.resolve(fileName), LOG_CHARSET, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        currentFileSizeBytes = 0;

        cleanupOldLogs();
    }

    /**
     * Cleans up old log files if total size exceeds the maximum limit.
     */
    private void cleanupOldLogs() throws IOException {
        long maxBytes = maxTotalLogsMB.get() * 1024L * 1024L;

        List<LogFileEntry> logFiles = new ArrayList<>();

        try (var stream = Files.list(PACKET_LOGS_DIR)) {
            for (Path p : stream.toList()) {
                String name = p.getFileName().toString();
                if (!name.startsWith("packets-") || !name.endsWith(".log")) continue;

                try {
                    logFiles.add(new LogFileEntry(
                        p,
                        Files.size(p),
                        Files.getLastModifiedTime(p).toMillis()
                    ));
                } catch (IOException ignored) {
                    // Skip unreadable files
                }
            }
        }

        logFiles.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        long totalSize = 0;
        for (LogFileEntry entry : logFiles) {
            totalSize += entry.size();
            if (totalSize > maxBytes) {
                Files.deleteIfExists(entry.path());
            }
        }
    }

    private record LogFileEntry(Path path, long size, long lastModified) {
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
