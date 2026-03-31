/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.debug.ChunkBorderRenderer;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChunkBorderRenderer.class)
public abstract class ChunkBorderRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/SectionPos;of(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/SectionPos;"))
    private SectionPos render$getChunkPos(SectionPos original) {
        Freecam freecam = Modules.get().get(Freecam.class);
        if (!freecam.isActive()) return original;

        float delta = minecraft.getRenderTickCounter().getTickProgress(true);

        return SectionPos.from(
            SectionPos.getSectionCoord(Mth.floor(freecam.getX(delta))),
            SectionPos.getSectionCoord(Mth.floor(freecam.getY(delta))),
            SectionPos.getSectionCoord(Mth.floor(freecam.getZ(delta)))
        );
    }
}
