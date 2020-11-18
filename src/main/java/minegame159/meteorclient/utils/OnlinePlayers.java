package minegame159.meteorclient.utils;

public class OnlinePlayers {
    private static long lastPingTime;

    public static void update() {
        long time = System.currentTimeMillis();

        if (time - lastPingTime > 5 * 60 * 1000) {
            System.out.println("Sending online ping");
            MeteorExecutor.execute(() -> HttpUtils.get("http://meteorclient.com:8082/api/online/ping"));
            lastPingTime = time;
        }
    }

    public static void leave() {
        System.out.println("Sending online leave");
        MeteorExecutor.execute(() -> HttpUtils.get("http://meteorclient.com:8082/api/online/leave"));
    }
}
