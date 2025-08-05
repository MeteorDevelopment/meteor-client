/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Chams;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.command.EntityRenderCommandQueue;
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

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/entity/command/EntityRenderCommandQueue;)V", at = @At("HEAD"))
    private void render$renderLayer(EndCrystalEntityRenderState endCrystalEntityRenderState, MatrixStack matrixStack, EntityRenderCommandQueue entityRenderCommandQueue, CallbackInfo ci) {
        END_CRYSTAL = RenderLayer.getEntityTranslucent((chams.isActive() && chams.crystals.get() && !chams.crystalsTexture.get()) ? Chams.BLANK : TEXTURE);
    }

    // Chams - Scale

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/entity/command/EntityRenderCommandQueue;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V"))
    private void render$scale(EndCrystalEntityRenderState endCrystalEntityRenderState, MatrixStack matrixStack, EntityRenderCommandQueue entityRenderCommandQueue, CallbackInfo ci) {
        if (!chams.isActive() || !chams.crystals.get()) return;

        float v = chams.crystalsScale.get().floatValue();
        matrixStack.scale(v, v, v);
    }

    // Chams - Color

    @WrapWithCondition(method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/entity/command/EntityRenderCommandQueue;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/command/EntityRenderCommandQueue;pushModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;III)V"))
    private boolean render$color(EntityRenderCommandQueue instance, Model model, Object state, MatrixStack matrices, RenderLayer renderLayer, int light, int overlay, int outlineColor) {
        if (chams.isActive() && chams.crystals.get()) {
            instance.pushModel(model,
                state,
                matrices,
                END_CRYSTAL,
                ((EndCrystalEntityRenderState) state).light,
                OverlayTexture.DEFAULT_UV,
                chams.crystalsColor.get().getPacked(),
                null,
                ((EndCrystalEntityRenderState) state).outlineColor,
                0);
            return false;
        }

        return true;
    }
}
