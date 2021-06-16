/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.network;

public class OnlinePlayers {
    private static long lastPingTime;

    public static void update() {
        long time = System.currentTimeMillis();

        if (time - lastPingTime > 5 * 60 * 1000) {
            MeteorExecutor.execute(() -> HttpUtils.post("http://meteorclient.com/api/online/ping"));

            lastPingTime = time;
        }
    }

    public static void leave() {
        MeteorExecutor.execute(() -> HttpUtils.post("http://meteorclient.com/api/online/leave"));
    }
}
