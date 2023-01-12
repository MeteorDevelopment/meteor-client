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
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;

public class WeatherChanger extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> rainGradient = sgGeneral.add(new DoubleSetting.Builder()
            .name("rain-gradient")
            .description("The specified rain gradient to be set.")
            .defaultValue(0)
            .sliderMin(0)
            .sliderMax(1)
            .build()
    );

    private final Setting<Double> thunderGradient = sgGeneral.add(new DoubleSetting.Builder()
            .name("thunder-gradient")
            .description("The specified thunder gradient to be set.")
            .defaultValue(0)
            .sliderMin(0)
            .sliderMax(1)
            .build()
    );

    float oldRainGradient;
    float oldThunderGradient;

    public WeatherChanger() {
        super(Categories.Render,"weather-changer", "Allows you to set a custom weather client-side.");
    }

    @Override
    public void onActivate() {
        oldRainGradient = mc.world.getRainGradient(0);
        oldThunderGradient = mc.world.getThunderGradient(0);
    }

    @Override
    public void onDeactivate() {
        mc.world.setRainGradient(oldRainGradient);
        mc.world.setThunderGradient(oldThunderGradient);
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!(event.packet instanceof GameStateChangeS2CPacket packet)) return;

        GameStateChangeS2CPacket.Reason reason = packet.getReason();
        if (reason == GameStateChangeS2CPacket.RAIN_STARTED) oldRainGradient = 1;
        else if (reason == GameStateChangeS2CPacket.RAIN_STOPPED) oldRainGradient = 0;
        else if (reason == GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED) oldRainGradient = packet.getValue();
        else if (reason == GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED) oldThunderGradient = packet.getValue();
        else return;


        event.setCancelled(true);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        mc.world.setRainGradient(rainGradient.get().floatValue());
        mc.world.setThunderGradient(thunderGradient.get().floatValue());
    }
}
