/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Chams;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.model.EndCrystalEntityModel;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndCrystalEntityRenderer.class)
public abstract class EndCrystalEntityRendererMixin {
    // Chams

    @Unique
    private Chams chams;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo info) {
        chams = Modules.get().get(Chams.class);
    }

    // Chams - Texture

    @Shadow
    @Final
    @Mutable
    private static RenderLayer END_CRYSTAL;

    @Shadow
    @Final
    private static Identifier TEXTURE;

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
    private void render$renderLayer(EndCrystalEntityRenderState endCrystalEntityRenderState, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        END_CRYSTAL = RenderLayer.getEntityTranslucent((chams.isActive() && chams.crystals.get() && !chams.crystalsTexture.get()) ? Chams.BLANK : TEXTURE);
    }

    // Chams - Scale

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V"))
    private void render$scale(EndCrystalEntityRenderState endCrystalEntityRenderState, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo info) {
        if (!chams.isActive() || !chams.crystals.get()) return;

        float v = chams.crystalsScale.get().floatValue();
        matrixStack.scale(v, v, v);
    }

    // Chams - Color

    @Shadow
    @Final
    private EndCrystalEntityModel model;

    @WrapWithCondition(method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EndCrystalEntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V"))
    private boolean render$color(EndCrystalEntityModel instance, MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        if (chams.isActive() && chams.crystals.get()) {
            model.render(matrices, vertices, light, overlay, chams.crystalsColor.get().getPacked());
            return false;
        }

        return true;
    }
}
