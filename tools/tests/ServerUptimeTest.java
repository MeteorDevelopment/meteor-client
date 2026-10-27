/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

import meteordevelopment.meteorclient.utils.network.ServerUptime;

// Run after building: java -ea -cp build/classes/java/main tools/tests/ServerUptimeTest.java
public class ServerUptimeTest {
    private static final long SECOND = 1_000_000_000L;

    public static void main(String[] args) {
        var clock = new ServerUptime();
        assert clock.getUptimeMillis(0) == -1;
        clock.observe(86_400_000, 0);
        clock.observe(86_415_000, 15 * SECOND);
        assert clock.getUptimeMillis(15 * SECOND) == -1 : "Require three consistent keepalives.";
        clock.observe(86_430_000, 30 * SECOND);
        assert clock.getClockType() == ServerUptime.ClockType.Milliseconds;
        assert clock.getUptimeMillis(35 * SECOND) == 86_435_000;
        assert clock.getUptimeMillis(91 * SECOND) == -1 : "Reject stale samples.";
        assert clock.getUptimeMillis(29 * SECOND) == -1;

        var nanos = samples(86400 * SECOND, 86415 * SECOND, 86430 * SECOND);
        assert nanos.getClockType() == ServerUptime.ClockType.Nanoseconds;
        assert nanos.getUptimeMillis(35 * SECOND) == 86_435_000;
        var epoch = samples(1_780_000_000_000L, 1_780_000_015_000L, 1_780_000_030_000L);
        assert epoch.getClockType() == ServerUptime.ClockType.Epoch;
        assert epoch.getUptimeMillis(30 * SECOND) == -1;

        for (long[] ids : new long[][]{
            {1, 2, 3}, {500000, 10, 987654321}, {10000, 10000, 10000}, {-30000, -15000, -1},
            {Long.MIN_VALUE, 1, Long.MAX_VALUE},
            {1_780_000_000_000_000_000L, 1_780_000_015_000_000_000L, 1_780_000_030_000_000_000L}
        }) {
            assert samples(ids).getUptimeMillis(30 * SECOND) == -1 : "Reject unsupported IDs.";
        }

        var jitter = new ServerUptime();
        jitter.observe(100000, 0);
        jitter.observe(115000, 14 * SECOND);
        jitter.observe(130000, 31 * SECOND);
        assert jitter.getUptimeMillis(31 * SECOND) == 130000;
        var burst = new ServerUptime();
        for (int i = 0; i < 3; i++) burst.observe(100000 + 15000 * i, i * SECOND / 1000);
        assert burst.getUptimeMillis(SECOND) == -1 : "Reject packet bursts.";

        clock.observe(1000, 45 * SECOND);
        assert clock.getUptimeMillis(45 * SECOND) == -1;
        clock.observe(16000, 60 * SECOND);
        assert clock.getUptimeMillis(60 * SECOND) == -1;
        clock.observe(31000, 75 * SECOND);
        assert clock.getUptimeMillis(75 * SECOND) == 31000 : "Recover after clock changes.";
        clock.reset();
        assert clock.getSamples() == 0 && clock.getUptimeMillis(75 * SECOND) == -1;
        for (int i = 0; i < 100000; i++) clock.observe(100000L + 15000L * i, i * 15 * SECOND);
        assert clock.getSamples() == 3 : "Keep sample state bounded.";
        System.out.println("ServerUptime checks passed.");
    }

    private static ServerUptime samples(long... ids) {
        var clock = new ServerUptime();
        for (int i = 0; i < ids.length; i++) clock.observe(ids[i], i * 15 * SECOND);
        return clock;
    }
}
