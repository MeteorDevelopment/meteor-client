/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import net.minecraft.client.MinecraftClient;

public class Fullbright extends Module {
    public Fullbright() {
        super(Categories.Render, "fullbright", "Lights up your world!");

        MeteorClient.EVENT_BUS.subscribe(StaticListener.class);
    }

    @Override
    public void onActivate() {
        enable();
    }

    @Override
    public void onDeactivate() {
        disable();
    }

    public static void enable() {
        StaticListener.timesEnabled++;
    }

    public static void disable() {
        StaticListener.timesEnabled--;
    }

    private static class StaticListener {
        private static final MinecraftClient mc = MinecraftClient.getInstance();

        private static int timesEnabled;
        private static int lastTimesEnabled;

        private static double prevGamma;

        @EventHandler
        private static void onTick(TickEvent.Post event) {
            if (timesEnabled > 0 && lastTimesEnabled == 0) {
                prevGamma = mc.options.gamma;
            }
            else if (timesEnabled == 0 && lastTimesEnabled > 0) {
                mc.options.gamma = prevGamma == 16 ? 1 : prevGamma;
            }

            if (timesEnabled > 0) {
                mc.options.gamma = 16;
            }

            lastTimesEnabled = timesEnabled;
        }
    }
}
