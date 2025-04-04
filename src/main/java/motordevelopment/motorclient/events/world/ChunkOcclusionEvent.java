/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.events.world;

import motordevelopment.motorclient.events.Cancellable;

public class ChunkOcclusionEvent extends Cancellable {
    private static final ChunkOcclusionEvent INSTANCE = new ChunkOcclusionEvent();

    public static ChunkOcclusionEvent get() {
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }
}
