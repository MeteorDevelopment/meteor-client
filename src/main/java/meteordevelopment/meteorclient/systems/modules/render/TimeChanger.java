/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

public class TimeChanger extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> time = sgGeneral.add(new IntSetting.Builder()
        .name("time")
        .description("The specified time to be set.")
        .defaultValue(0)
        .sliderRange(-20000, 20000)
        .build()
    );

    private final Setting<Boolean> controlSpeed = sgGeneral.add(new BoolSetting.Builder()
        .name("control-speed")
        .description("Whether to control the rate at which time passes.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> speed = sgGeneral.add(new IntSetting.Builder()
        .name("speed")
        .description("The rate at which the time passes.")
        .defaultValue(0)
        .sliderRange(-100, 100)
        .visible(controlSpeed::get)
        .build()
    );

    long newTime;
    long oldTime;

    public TimeChanger() {
        super(Categories.Render, "time-changer", "Makes you able to control the time.");
    }

    @Override
    public void onActivate() {
        newTime = mc.world.getTime();
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
            event.cancel();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (controlSpeed.get()) {
            mc.world.setTimeOfDay(newTime += speed.get());
        } else {
            mc.world.setTimeOfDay(time.get());
        }
    }
}
