/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.atomic.AtomicReferenceArray;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(targets = "net.minecraft.client.multiplayer.ClientChunkCache$Storage")
public interface ClientChunkMapAccessor {
    @Accessor("chunks")
    AtomicReferenceArray<LevelChunk> meteor$getChunks();
}
