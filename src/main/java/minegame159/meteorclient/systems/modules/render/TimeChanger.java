/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.packets.PacketEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

public class TimeChanger extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();

    private final Setting<Double> time = sgDefault.add(new DoubleSetting.Builder()
            .name("time")
            .description("The specified time to be set.")
            .defaultValue(0)
            .sliderMin(-20000)
            .sliderMax(20000)
            .build()
    );

    long oldTime;

    public TimeChanger() {
        super(Categories.Render, "time-changer", "Makes you able to set a custom time.");
    }

    @Override
    public void onActivate() {
        oldTime = mc.world.getTime();
    }

    @Override
    public void onDeactivate() {
        mc.world.setTimeOfDay(oldTime);
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof WorldTimeUpdateS2CPacket) {
            oldTime = ((WorldTimeUpdateS2CPacket) event.packet).getTime();
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        mc.world.setTimeOfDay(time.get().longValue());
    }
}
