/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.PacketUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;

import java.util.ArrayList;
import java.util.Set;

public class PacketDelay extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Set<Class<? extends Packet<?>>>> s2cPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("S2C-packets")
        .description("Server-to-client packets to delay.")
        .filter(aClass -> PacketUtils.getS2CPackets().contains(aClass))
        .build()
    );

    private final Setting<Set<Class<? extends Packet<?>>>> c2sPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("C2S-packets")
        .description("Client-to-server packets to delay.")
        .filter(aClass -> PacketUtils.getC2SPackets().contains(aClass))
        .build()
    );

    private final Setting<Boolean> timeDelay = sgGeneral.add(new BoolSetting.Builder()
        .name("time-delay")
        .description("Whether to allow packets after a set time.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("How long to wait in ticks before allowing a packet.")
        .min(0)
        .noSlider()
        .visible(timeDelay::get)
        .build()
    );

    private ArrayList<PacketAndTime> s2cPacketQueue;
    private ArrayList<PacketAndTime> c2sPacketQueue;

    public PacketDelay() {
        super(Categories.Misc, "packet-delay", "Allows you to delay certain packets.");
    }

    @Override
    public void onActivate() {
        s2cPacketQueue = new ArrayList<>();
        c2sPacketQueue = new ArrayList<>();
    }

    @Override
    public void onDeactivate() {
        for (PacketAndTime packetAndTime : s2cPacketQueue) {
            applyPacket(packetAndTime.packet);
        }

        for (PacketAndTime packetAndTime : c2sPacketQueue) {
            mc.getNetworkHandler().getConnection().send(packetAndTime.packet, null);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (timeDelay.get()) {
            if (!s2cPacketQueue.isEmpty()) {
                for (int i = 0; i < s2cPacketQueue.size(); i++) {
                    PacketAndTime packetAndTime = s2cPacketQueue.get(i);
                    if (packetAndTime.time <= mc.world.getTime() - delay.get()) {
                        applyPacket(packetAndTime.packet);
                        s2cPacketQueue.remove(i);
                        i--;
                    } else break;
                }
            }

            if (!c2sPacketQueue.isEmpty()) {
                for (int i = 0; i < c2sPacketQueue.size(); i++) {
                    PacketAndTime packetAndTime = c2sPacketQueue.get(i);
                    if (packetAndTime.time <= mc.world.getTime() - delay.get()) {
                        mc.getNetworkHandler().getConnection().send(packetAndTime.packet, null);
                        c2sPacketQueue.remove(i);
                        i--;
                    } else break;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onReceivePacket(PacketEvent.Receive event) {
        if (s2cPackets.get().contains(event.packet.getClass())) {
            s2cPacketQueue.add(new PacketAndTime(event.packet, mc.world.getTime()));
            event.cancel();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onSendPacket(PacketEvent.Send event) {
        if (c2sPackets.get().contains(event.packet.getClass())) {
            c2sPacketQueue.add(new PacketAndTime(event.packet, mc.world.getTime()));
            event.cancel();
        }
    }

    private <T extends PacketListener> void applyPacket(Packet<T> packet) {
        packet.apply((T) mc.getNetworkHandler().getConnection().getPacketListener());
    }

    private record PacketAndTime(Packet<?> packet, long time) {}
}
