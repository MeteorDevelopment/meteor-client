/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import java.util.Set;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.PacketListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.PacketUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.packet.Packet;

public class PacketCanceller extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Set<Class<? extends Packet<?>>>> s2cPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("S2C-packets")
        .description("Server-to-client packets to cancel.")
        .filter(aClass -> PacketUtils.getS2CPackets().contains(aClass))
        .build()
    );

    private final Setting<Set<Class<? extends Packet<?>>>> c2sPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("C2S-packets")
        .description("Client-to-server packets to cancel.")
        .filter(aClass -> PacketUtils.getC2SPackets().contains(aClass))
        .build()
    );

    private final SettingGroup sgLog = settings.createGroup("Log");

    private final Setting<Boolean> logPackets = sgLog.add(new BoolSetting.Builder()
        .name("log-packets")
        .description("Log all packets.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> logCancelled = sgLog.add(new BoolSetting.Builder()
        .name("cancelled")
        .description("Only log cancelled packets.")
        .defaultValue(true)
        .visible(logPackets::get)
        .build()
    );

    public PacketCanceller() {
        super(Categories.Misc, "packet-canceller", "Allows you to cancel certain packets.");
        runInMainMenu = true;
    }

    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onReceivePacket(PacketEvent.Receive event) {
        if (s2cPackets.get().contains(event.packet.getClass())) event.cancel();
        if(logPackets.get() && (!logCancelled.get() || event.isCancelled()))
            MeteorClient.LOG.info((event.isCancelled() ? "Drop " : "") + "Packet.Receive {}", event.packet.getPacketType());
    }

    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onSendPacket(PacketEvent.Send event) {
        if (c2sPackets.get().contains(event.packet.getClass())) event.cancel();
        if(logPackets.get() && (logCancelled.get() && event.isCancelled()))
            MeteorClient.LOG.info("Cancel Packet.Send {}", event.packet.getPacketType());
    }

    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onSentPacket(PacketEvent.Sent event) {
        if(logPackets.get())
            MeteorClient.LOG.info("Packet.Sent {}", event.packet.getPacketType());
    }
}
