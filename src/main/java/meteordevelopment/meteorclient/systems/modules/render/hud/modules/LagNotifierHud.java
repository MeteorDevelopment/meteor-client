/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.hud.modules;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUD;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.TickRate;

public class LagNotifierHud extends DoubleTextHudElement {
    private static final Color RED = new Color(225, 45, 45);
    private static final Color AMBER = new Color(235, 158, 52);
    private static final Color YELLOW = new Color(255, 255, 5);
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> yellowColor = sgGeneral.add(new DoubleSetting.Builder()
            .name("Counter Start")
            .description("Set time in seconds when the counter should appear.")
            .defaultValue(2)
            .min(0)
            .build()
    );
    
    private final Setting<Double> amberColor = sgGeneral.add(new DoubleSetting.Builder()
            .name("Warning Time")
            .description("When to set color to amber.")
            .defaultValue(5)
            .min(0)
            .sliderMax(30)
            .build()
    );
    
    private final Setting<Double> redColor = sgGeneral.add(new DoubleSetting.Builder()
            .name("Alert Time")
            .description("When to set color to red.")
            .defaultValue(10)
            .min(0)
            .sliderMax(30)
            .build()
    );
    
    public LagNotifierHud(HUD hud) {
        super(hud, "lag-notifier", "Displays if the server is lagging in ticks.", "Time since last tick ");
    }

    @Override
    protected String getRight() {
        if (isInEditor()) {
            rightColor = RED;
            visible = true;
            return "4,3";
        }

        float timeSinceLastTick = TickRate.INSTANCE.getTimeSinceLastTick();

        if (timeSinceLastTick > redColor.get()) rightColor = RED;
        else if (timeSinceLastTick > amberColor.get()) rightColor = AMBER;
        else rightColor = YELLOW;

        visible = timeSinceLastTick >= yellowColor.get();
        return String.format("%.1f", timeSinceLastTick);
    }
}
