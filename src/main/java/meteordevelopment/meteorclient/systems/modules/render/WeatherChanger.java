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
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;

public class WeatherChanger extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> rainLevel = sgGeneral.add(new DoubleSetting.Builder()
        .name("rain-level")
        .description("The specified rain level to be set.")
        .defaultValue(0)
        .sliderRange(0, 1)
        .build()
    );

    private final Setting<Double> thunderLevel = sgGeneral.add(new DoubleSetting.Builder()
        .name("thunder-level")
        .description("The specified thunder level to be set.")
        .defaultValue(0)
        .sliderRange(0, 1)
        .build()
    );

    private float oldThunderLevel;
    private float oldRainLevel;

    public WeatherChanger() {
        super(Categories.Render, "weather-changer", "Allows you to override the world's current weather.");
    }

    @Override
    public void onActivate() {
        if (mc.level == null) {
            return;
        }

        oldThunderLevel = mc.level.getThunderLevel(1f);
        oldRainLevel = mc.level.getRainLevel(1f);
    }

    @Override
    public void onDeactivate() {
        if (mc.level == null) {
            return;
        }

        mc.level.setRainLevel(oldRainLevel);
        mc.level.setThunderLevel(oldThunderLevel);
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundGameEventPacket packet) {
            ClientboundGameEventPacket.Type type = packet.getEvent();

            if (type == ClientboundGameEventPacket.START_RAINING
                || type == ClientboundGameEventPacket.STOP_RAINING
                || type == ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE
                || type == ClientboundGameEventPacket.RAIN_LEVEL_CHANGE) {

                if (type == ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE) {
                    oldThunderLevel = packet.getParam();
                } else if (type == ClientboundGameEventPacket.RAIN_LEVEL_CHANGE) {
                    oldRainLevel = packet.getParam();
                }

                event.cancel();
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.level == null) {
            return;
        }

        mc.level.setRainLevel(rainLevel.get().floatValue());
        mc.level.setThunderLevel(thunderLevel.get().floatValue());
    }
}
