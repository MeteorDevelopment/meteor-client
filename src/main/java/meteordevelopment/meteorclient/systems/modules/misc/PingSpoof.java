/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PingSpoof extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay for outgoing packets in milliseconds.")
        .defaultValue(200)
        .min(0)
        .sliderRange(0, 1000)
        .build()
    );

    private final Setting<Boolean> renderBox = sgGeneral.add(new BoolSetting.Builder()
        .name("render-server-box")
        .description("Render where the server thinks you are.")
        .defaultValue(true)
        .build()
    );

    private final Queue<DelayedPacket> queue = new ConcurrentLinkedQueue<>();
    private Vec3d serverPos = Vec3d.ZERO;

    public PingSpoof() {
        super(Categories.Misc, "ping-spoof", "Simulates network latency by delaying packets.");
    }

    @Override
    public void onActivate() {
        if (mc.player != null) serverPos = mc.player.getPos();
    }

    @Override
    public void onDeactivate() {
        flushQueue();
    }

    @EventHandler(priority = EventPriority.HIGHEST + 50)
    private void onSendPacket(PacketEvent.Send event) {
        if (!Utils.canUpdate()) return;
        queue.add(new DelayedPacket(event.packet, event.connection, System.currentTimeMillis() + delay.get()));
        event.cancel();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        long now = System.currentTimeMillis();
        while (true) {
            DelayedPacket p = queue.peek();
            if (p == null || p.time > now) break;
            queue.poll();
            if (p.packet instanceof PlayerMoveC2SPacket move) {
                double x = move.getX(serverPos.x);
                double y = move.getY(serverPos.y);
                double z = move.getZ(serverPos.z);
                serverPos = new Vec3d(x, y, z);
            }
            p.connection.send(p.packet, null, true);
        }
    }

    private void flushQueue() {
        DelayedPacket p;
        while ((p = queue.poll()) != null) {
            p.connection.send(p.packet, null, true);
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!renderBox.get() || mc.player == null) return;
        Box box = mc.player.getBoundingBox().offset(
            serverPos.x - mc.player.getX(),
            serverPos.y - mc.player.getY(),
            serverPos.z - mc.player.getZ()
        );
        event.renderer.box(box, Color.RED, Color.RED, ShapeMode.Lines, 0);
    }

    private static class DelayedPacket {
        final Packet<?> packet;
        final ClientConnection connection;
        final long time;

        DelayedPacket(Packet<?> packet, ClientConnection connection, long time) {
            this.packet = packet;
            this.connection = connection;
            this.time = time;
        }
    }
}
