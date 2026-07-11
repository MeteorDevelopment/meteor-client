/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.sodium;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = ChunkBuilderMeshingTask.class, remap = false)
public abstract class SodiumChunkBuilderMeshingTaskMixin {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static SortBehavior modifySortBehavior(SortBehavior sortBehavior) {
        return Modules.get().isActive(Xray.class) && Modules.get().get(Xray.class).opacity.get() > 0 && Modules.get().get(Xray.class).opacity.get() < 255 ? SortBehavior.STATIC : sortBehavior;
    }
}
