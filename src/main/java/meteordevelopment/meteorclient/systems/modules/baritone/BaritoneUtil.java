/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.baritone;

import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.systems.modules.Module;

/** Shared "is Baritone installed" guard for the one-shot modules in this package. */
final class BaritoneUtil {
    private BaritoneUtil() {
    }

    static boolean check(Module module) {
        if (!BaritoneUtils.IS_AVAILABLE) {
            module.error("Baritone is not installed.");
            return false;
        }

        return true;
    }
}
