/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.ChunkOcclusionEvent;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkOcclusionDataBuilder.class)
public class ChunkOcclusionDataBuilderMixin {
    @Inject(method = "markClosed", at = @At("HEAD"), cancellable = true)
    private void onMarkClosed(BlockPos pos, CallbackInfo info) {
        ChunkOcclusionEvent event = MeteorClient.EVENT_BUS.post(ChunkOcclusionEvent.get());
        if (event.isCancelled()) info.cancel();
    }
}
