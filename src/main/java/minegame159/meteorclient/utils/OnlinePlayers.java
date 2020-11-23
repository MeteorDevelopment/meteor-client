/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.utils;

public class OnlinePlayers {
    private static long lastPingTime;

    public static void update() {
        long time = System.currentTimeMillis();

        if (time - lastPingTime > 5 * 60 * 1000) {
            MeteorExecutor.execute(() -> HttpUtils.get("http://meteorclient.com:8082/api/online/ping"));
            lastPingTime = time;
        }
    }

    public static void leave() {
        MeteorExecutor.execute(() -> HttpUtils.get("http://meteorclient.com:8082/api/online/leave"));
    }
}
