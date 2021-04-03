/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.network;

import minegame159.meteorclient.systems.config.Config;
import net.minecraft.client.MinecraftClient;

public class OnlinePlayers {
    private static long lastPingTime;

    public static void update() {
        long time = System.currentTimeMillis();

        if (time - lastPingTime > 5 * 60 * 1000) {
            MeteorExecutor.execute(() -> {
                String url = "http://meteorclient.com/api/online/ping";

                String uuid = MinecraftClient.getInstance().getSession().getUuid();
                if (uuid != null && !uuid.isEmpty() && Config.get().sendDataToApi) url += "?uuid=" + uuid;

                HttpUtils.post(url);
            });

            lastPingTime = time;
        }
    }

    public static void forcePing() {
        lastPingTime = 0;
    }

    public static void leave() {
        MeteorExecutor.execute(() -> HttpUtils.post("http://meteorclient.com/api/online/leave"));
    }
}
