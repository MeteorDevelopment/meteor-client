/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 * Adapted from THM Addons (https://github.com/Leonn170709/THM-Addons).
 * Copyright (c) THM Addons contributors.
 */

package meteordevelopment.meteorclient.utils.network;

public class ServerUptime {
    public enum ClockType { Collecting, Milliseconds, Nanoseconds, Epoch, Unsupported }

    private static final long MAX_UPTIME_MS = 30L * 366 * 24 * 60 * 60 * 1000;
    private long lastId, lastReceivedNs;
    private int samples, matchingIntervals;
    private ClockType candidate = ClockType.Collecting;

    public void reset() {
        lastId = lastReceivedNs = 0;
        samples = matchingIntervals = 0;
        candidate = ClockType.Collecting;
    }

    public void observe(long id, long receivedNs) {
        if (samples > 0) {
            double elapsedMs = (receivedNs - lastReceivedNs) / 1_000_000.0;
            double delta = (double) id - lastId;
            ClockType next = ClockType.Unsupported;
            if (id > 0 && lastId > 0 && elapsedMs >= 100 && delta > 0) {
                if (matches(delta, elapsedMs)) next = id < MAX_UPTIME_MS ? ClockType.Milliseconds : ClockType.Epoch;
                else if (matches(delta / 1_000_000.0, elapsedMs) && id / 1_000_000 < MAX_UPTIME_MS) next = ClockType.Nanoseconds;
            }
            matchingIntervals = next == candidate ? Math.min(2, matchingIntervals + 1) : 1;
            candidate = next;
        }
        lastId = id;
        lastReceivedNs = receivedNs;
        samples = Math.min(3, samples + 1);
    }

    private static boolean matches(double deltaMs, double elapsedMs) {
        return Math.abs(deltaMs - elapsedMs) <= Math.max(25, elapsedMs * .2);
    }

    public ClockType getClockType() {
        return matchingIntervals >= 2 ? candidate : ClockType.Collecting;
    }

    public int getSamples() {
        return samples;
    }

    public long getUptimeMillis(long nowNs) {
        long age = (nowNs - lastReceivedNs) / 1_000_000;
        if (samples == 0 || age < 0 || age > 60_000) return -1;
        // A monotonic clock's origin is JVM/platform-dependent.
        return switch (getClockType()) {
            case Milliseconds -> lastId + age;
            case Nanoseconds -> lastId / 1_000_000 + age;
            default -> -1;
        };
    }
}
