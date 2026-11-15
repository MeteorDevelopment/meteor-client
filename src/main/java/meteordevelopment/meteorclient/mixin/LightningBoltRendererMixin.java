/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Ambience;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.renderer.entity.LightningBoltRenderer;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningBoltRenderer.class)
public abstract class LightningBoltRendererMixin {
    /**
     * @author Walaryne
     */
    @Inject(method = "quad", at = @At(value = "HEAD"), cancellable = true)
    private static void onSetLightningVertex(Matrix4fc pose, VertexConsumer buffer, float segmentStartX, float segmentStartZ, float segmentEndX, float segmentEndZ, int currentSegment, float topRadius, float bottomRadius, boolean rightXPositive, boolean rightZPositive, boolean leftXPositive, boolean leftZPositive, CallbackInfo ci) {
        Ambience ambience = Modules.get().get(Ambience.class);

        if (ambience.isActive() && ambience.changeLightningColor.get()) {
            Color color = ambience.lightningColor.get();

            buffer.addVertex(pose, segmentStartX + (rightXPositive ? bottomRadius : -bottomRadius), currentSegment * 16, segmentStartZ + (rightZPositive ? bottomRadius : -bottomRadius)).setColor(color.r / 255f, color.g / 255f, color.b / 255f, 0.3F);
            buffer.addVertex(pose, segmentEndX + (rightXPositive ? topRadius : -topRadius), (currentSegment + 1) * 16, segmentEndZ + (rightZPositive ? topRadius : -topRadius)).setColor(color.r / 255f, color.g / 255f, color.b / 255f, 0.3F);
            buffer.addVertex(pose, segmentEndX + (leftXPositive ? topRadius : -topRadius), (currentSegment + 1) * 16, segmentEndZ + (leftZPositive ? topRadius : -topRadius)).setColor(color.r / 255f, color.g / 255f, color.b / 255f, 0.3F);
            buffer.addVertex(pose, segmentStartX + (leftXPositive ? bottomRadius : -bottomRadius), currentSegment * 16, segmentStartZ + (leftZPositive ? bottomRadius : -bottomRadius)).setColor(color.r / 255f, color.g / 255f, color.b / 255f, 0.3F);

            ci.cancel();
        }
    }
}
