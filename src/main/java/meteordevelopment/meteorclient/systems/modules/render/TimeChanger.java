/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.clock.ClockNetworkState;

import java.util.Map;

public class TimeChanger extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> time = sgGeneral.add(new DoubleSetting.Builder()
        .name("time")
        .description("The specified time to be set.")
        .defaultValue(0)
        .sliderRange(-20000, 20000)
        .build()
    );

    long oldTime;

    public TimeChanger() {
        super(Categories.Render, "time-changer", "Makes you able to set a custom time.");
    }

    @Override
    public void onActivate() {
        if (mc.level != null) oldTime = mc.level.getDefaultClockTime();
    }

    @Override
    public void onDeactivate() {
        if (mc.level == null) return;

        mc.level.dimensionType().defaultClock().ifPresent(clock -> {
            mc.level.clockManager().handleUpdates(mc.level.getGameTime(), Map.of(clock, new ClockNetworkState(oldTime, 0f, 1f)));
            mc.level.environmentAttributes().invalidateTickCache();
        });
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundSetTimePacket packet) {
            if (mc.level == null) return;

            mc.execute(() -> {
                if (mc.level != null) {
                    mc.level.setTimeFromServer(packet.gameTime());
                    mc.level.dimensionType().defaultClock().ifPresent(clock -> {
                        ClockNetworkState state = packet.clockUpdates().get(clock);
                        if (state != null) oldTime = state.totalTicks();
                    });
                }
            });
            event.cancel();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.level == null) return;

        long customTime = time.get().longValue();
        mc.level.dimensionType().defaultClock().ifPresent(clock -> {
            mc.level.clockManager().handleUpdates(mc.level.getGameTime(), Map.of(clock, new ClockNetworkState(customTime, 0f, 0f)));
            mc.level.environmentAttributes().invalidateTickCache();
        });
    }
}
