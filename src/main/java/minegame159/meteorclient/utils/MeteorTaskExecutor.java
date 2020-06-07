package minegame159.meteorclient.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MeteorTaskExecutor {
    private static ExecutorService executor;
    private static int startedCount = 0;

    private static final List<Runnable> tasks = new ArrayList<>();

    public static void start() {
        synchronized (tasks) {
            if (startedCount == 0) {
                executor = Executors.newSingleThreadExecutor();

                for (Runnable task : tasks) executor.execute(task);
                tasks.clear();
            }

            startedCount++;
        }
    }

    public static void stop() {
        synchronized (tasks) {
            if (startedCount > 0) {
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
        }
    }

    public static void execute(Runnable task) {
        synchronized (tasks) {
            if (executor == null) tasks.add(task);
            else executor.execute(task);
        }
    }
}
