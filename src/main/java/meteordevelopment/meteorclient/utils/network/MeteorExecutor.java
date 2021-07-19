/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.network;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MeteorExecutor {
    public static ExecutorService executor;

    public static void init() {
        executor = Executors.newSingleThreadExecutor();
    }

    public static void execute(Runnable task) {
        executor.execute(task);
    }
}
