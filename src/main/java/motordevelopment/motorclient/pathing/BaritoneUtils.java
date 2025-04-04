/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.pathing;

import baritone.api.BaritoneAPI;

public class BaritoneUtils {
    public static boolean IS_AVAILABLE = false;

    private BaritoneUtils() {
    }

    public static String getPrefix() {
        if (IS_AVAILABLE) {
            return BaritoneAPI.getSettings().prefix.value;
        }

        return "";
    }
}
