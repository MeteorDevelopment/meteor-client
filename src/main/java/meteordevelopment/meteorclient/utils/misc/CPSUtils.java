/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;


public class CPSUtils {
    private static int clicks;
    private static int cps;
    private static int secondsClicking;

    private static int rightClicks;
    private static int rightCps;
    private static int rightSecondsClicking;

    private static long lastTime;

    @PreInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(CPSUtils.class);
    }

    @EventHandler
    public static void onClick(MouseButtonEvent event) {
        if (event.button == 0 && event.action == KeyAction.Press) {
            CPSUtils.clicks++;
            CPSUtils.cps++;
        } else if (event.button == 1 && event.action == KeyAction.Press) {
            CPSUtils.rightClicks++;
            CPSUtils.rightCps++;
        }
    }

    @EventHandler
    private static void onTick(TickEvent.Pre event) {
        long currentTime = System.currentTimeMillis();
        // Run every second
        if (currentTime - CPSUtils.lastTime >= 1000) {
            CPSUtils.lastTime = currentTime;

            // Left
            if (CPSUtils.cps == 0) {
                CPSUtils.clicks = 0;
                CPSUtils.secondsClicking = 0;
            } else {
                CPSUtils.secondsClicking++;
                CPSUtils.cps = 0;
            }

            // Right
            if (CPSUtils.rightCps == 0) {
                CPSUtils.rightClicks = 0;
                CPSUtils.rightSecondsClicking = 0;
            } else {
                CPSUtils.rightSecondsClicking++;
                CPSUtils.rightCps = 0;
            }
        }
    }

    public static int getCpsAverage() {
        return clicks / (secondsClicking == 0 ? 1 : secondsClicking);
    }
    public static int getRightCpsAverage() {
        return rightClicks / (rightSecondsClicking == 0 ? 1 : rightSecondsClicking);
    }
}
