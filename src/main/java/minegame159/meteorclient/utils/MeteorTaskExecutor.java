package minegame159.meteorclient.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MeteorTaskExecutor {
    private static ExecutorService executor;
    private static int startedCount = 0;

    public static void start() {
        if (startedCount == 0) {
            executor = Executors.newSingleThreadExecutor();
        }

        startedCount++;
    }

    public static void stop() {
        startedCount--;

        if (startedCount == 0) {
            try {
                executor.shutdown();
                executor.awaitTermination(5, TimeUnit.SECONDS);
                executor = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void execute(Runnable task) {
        executor.execute(task);
    }
}
