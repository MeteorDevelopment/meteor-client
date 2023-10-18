/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.pathing;

import meteordevelopment.meteorclient.utils.PreInit;

public class PathManagers {
    private static IPathManager INSTANCE = new NopPathManager();

    public static IPathManager get() {
        return INSTANCE;
    }

    @PreInit
    public static void init() {
        try {
            Class.forName("baritone.api.BaritoneAPI");

            BaritoneUtils.IS_AVAILABLE = true;
            INSTANCE = new BaritonePathManager();
        } catch (ClassNotFoundException ignored) {}
    }
}
