/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.events.packets.SendPacketEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.ArrayList;
import java.util.List;

public class Blink extends ToggleModule {
    public Blink() {
        super(Category.Movement, "blink", "Suspends all motion updates while enabled.");
    }

    private final List<PlayerMoveC2SPacket> packets = new ArrayList<>();
    private int timer = 0;

    @Override
    public void onDeactivate() {
        synchronized (packets) {
            packets.forEach(p -> mc.player.networkHandler.sendPacket(p));
            packets.clear();
            timer = 0;
        }
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> timer++);

    @EventHandler
    private final Listener<SendPacketEvent> onSendPacket = new Listener<>(event -> {
        if (!(event.packet instanceof PlayerMoveC2SPacket)) return;
        event.cancel();

        synchronized (packets) {
            PlayerMoveC2SPacket p = (PlayerMoveC2SPacket) event.packet;
            PlayerMoveC2SPacket prev = packets.size() == 0 ? null : packets.get(packets.size() - 1);

            if (prev != null &&
                    p.isOnGround() == prev.isOnGround() &&
                    p.getYaw(-1) == prev.getYaw(-1) &&
                    p.getPitch(-1) == prev.getPitch(-1) &&
                    p.getX(-1) == prev.getX(-1) &&
                    p.getY(-1) == prev.getY(-1) &&
                    p.getZ(-1) == prev.getZ(-1)
            ) return;

            packets.add(p);
        }
    });

    @Override
    public String getInfoString() {
        return String.format("%.1f", timer / 20f);
    }
}
