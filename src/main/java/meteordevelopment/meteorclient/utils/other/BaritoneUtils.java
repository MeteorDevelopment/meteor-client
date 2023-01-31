/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.other;

import baritone.api.BaritoneAPI;
import net.fabricmc.loader.api.FabricLoader;

public class BaritoneUtils {
    public static String getPrefix() {
        return BaritoneAPI.getSettings().prefix.value;
    }

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded("baritone");
    }
}
