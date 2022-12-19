/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.PacketListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.PacketUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.Packet;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

public class PacketLogger extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

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

    public PacketLogger() {
        super(Categories.Misc, "packet-logger", "Logs specific packets to console");
    }

    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onReceivePacket(PacketEvent.Receive event) {
        if (s2cPackets.get().contains(event.packet.getClass())) log("incomming", event.packet);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onSendPacket(PacketEvent.Send event) {
        if (c2sPackets.get().contains(event.packet.getClass())) log("outgoing", event.packet);
    }

    private void log(String prefix, Packet<?> packet) {
        MeteorClient.LOG.info("Packet {} {}", prefix, packetToString(packet));
    }

    /**
     * Returns a string representation of the packet.
     * The string representation consists of the packet's name and a list of the packet's values, enclosed in square brackets ("[]").
     * Adjacent values are separated by the characters ", " (a comma followed by a space).
     * Values are converted to strings as by {@link Objects#toString(Object)}
     *
     * @param packet the packet whose string representation to return
     * @return a string representation of packet
     */
    private String packetToString(Packet<?> packet) {
        String packetName = PacketUtils.getName((Class<? extends Packet<?>>) packet.getClass());

        try {
            StringJoiner values = new StringJoiner(", ", "[", "]");
            Class<?> clazz = packet.getClass();

            // needed
            while (clazz.getDeclaredFields().length == 0 && packet.getClass().getSuperclass() != null) {
                clazz = packet.getClass().getSuperclass();
            }

            for (Field f : clazz.getDeclaredFields()) {
                if (Modifier.isFinal(f.getModifiers()) && Modifier.isStatic(f.getModifiers()))
                    continue; // constant

                if (!f.canAccess(packet))
                    f.setAccessible(true);

                @Nullable Object value = f.get(Modifier.isStatic(f.getModifiers()) ? null : packet);
                values.add(Objects.toString(value));
            }

            return String.join(" ", packetName, values.toString());
        } catch (Exception e) {
            MeteorClient.LOG.error("Cannot construct packet values string", e);
            return packetName;
        }
    }
}
