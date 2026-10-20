/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

/**
 * Math helpers that do not depend on a specific Minecraft version.
 *
 * <p>{@code net.minecraft.util.Mth#clamp} was removed in Minecraft 26.4. The JDK's
 * {@code Math#clamp} is not a drop-in replacement for it: the JDK throws when
 * {@code min > max}, while vanilla simply returns the value unchanged. These overloads keep
 * the vanilla behaviour, which matters for call sites where the bounds can end up inverted
 * (e.g. a scroll range whose content is smaller than the viewport).
 */
public final class MathUtils {
    private MathUtils() {}

    public static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    public static long clamp(long value, long min, long max) {
        return Math.min(Math.max(value, min), max);
    }

    public static float clamp(float value, float min, float max) {
        return Math.min(Math.max(value, min), max);
    }

    public static double clamp(double value, double min, double max) {
        return Math.min(Math.max(value, min), max);
    }
}
