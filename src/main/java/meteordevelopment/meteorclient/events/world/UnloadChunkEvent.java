/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.world;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.world.chunk.WorldChunk;

public class UnloadChunkEvent extends Cancellable {
    private static final UnloadChunkEvent INSTANCE = new UnloadChunkEvent();

    public WorldChunk chunk;

    public static UnloadChunkEvent get(WorldChunk chunk) {
        INSTANCE.chunk = chunk;
        return INSTANCE;
    }
}
