/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.AbstractSignEditScreenAccessor;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;

import java.util.ArrayDeque;
import java.util.Queue;

public class AutoSign extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The tick delay between sign update packets.")
        .defaultValue(10)
        .range(0, 100)
        .sliderRange(0, 100)
        .build()
    );

    private String[] text;

    // Some servers (e.g., 2b2t) don't like the sign packet being sent too soon after the swing or block click packets, so queue them.
    // Delaying by sleeping in the event handler may be fine for a single sign, but would visibly lag the UI at a larger scale.
    private final Queue<UpdateSignC2SPacket> queue = new ArrayDeque<>();
    private int timer = 0;

    public AutoSign() {
        super(Categories.World, "auto-sign", "Automatically writes signs. The first sign's text will be used.");
    }

    @Override
    public void onDeactivate() {
        text = null;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        // Adding a new packet with the timer close to the threshold could lead to it being sent too fast relative to
        // the swing/click packets, but only if there isn't another packet ahead of it to reset the timer, so always
        // keep it reset if the queue is empty.
        if (mc.player == null || queue.peek() == null) {
            timer = 0;
            return;
        }

        if (timer < delay.get()) {
            timer++;
            return;
        }

        mc.player.networkHandler.sendPacket(queue.poll());

        timer = 0;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof UpdateSignC2SPacket)) return;

        text = ((UpdateSignC2SPacket) event.packet).getText();
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (!(event.screen instanceof AbstractSignEditScreen) || text == null) return;

        SignBlockEntity sign = ((AbstractSignEditScreenAccessor) event.screen).meteor$getSign();

        queue.add(new UpdateSignC2SPacket(sign.getPos(), true, text[0], text[1], text[2], text[3]));

        event.cancel();
    }
}
