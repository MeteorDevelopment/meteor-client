/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.misc;

public class ThreadUtils {

    /**
     * Runs a method in a thread. The method cannot return anything. A stack trace is dumped if an exception occurs
     *
     * @param runnable the method to run
     */
    public static void runInThread(Runnable runnable) {
        new Thread(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Makes a thread delay for the specified time.
     *
     * @param ms Time to sleep in milliseconds
     */
    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
