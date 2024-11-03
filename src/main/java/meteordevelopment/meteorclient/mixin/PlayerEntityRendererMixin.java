/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Chams;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    // Chams

    @Unique
    private Chams chams;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init$chams(CallbackInfo info) {
        chams = Modules.get().get(Chams.class);
    }

    // Chams - Player scale

    @Inject(method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("RETURN"))
    private void updateRenderState$scale(AbstractClientPlayerEntity player, PlayerEntityRenderState state, float f, CallbackInfo info) {
        if (!chams.isActive() || !chams.players.get()) return;
        if (chams.ignoreSelf.get() && player == MeteorClient.mc.player) return;

        float v = chams.playersScale.get().floatValue();
        state.baseScale *= v;

        //noinspection DataFlowIssue
        ((IVec3d) state.nameLabelPos).setY(state.nameLabelPos.y + (player.getHeight() * v - player.getHeight()));
    }

    // TODO: update
    /*@ModifyArgs(method = "renderArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPart;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V", ordinal = 0))
    private void modifyRenderLayer(Args args, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve) {
        Chams chams = Modules.get().get(Chams.class);

        if (chams.isActive() && chams.hand.get()) {
            Identifier texture = chams.handTexture.get() ? player.getSkinTextures().texture() : Chams.BLANK;
            args.set(1, vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(texture)));
        }
    }

    @Redirect(method = "renderArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPart;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V", ordinal = 0))
    private void redirectRenderMain(ModelPart modelPart, MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        Chams chams = Modules.get().get(Chams.class);

        if (chams.isActive() && chams.hand.get()) {
            Color color = chams.handColor.get();
            modelPart.render(matrices, vertices, light, overlay, color.getPacked());
        } else {
            modelPart.render(matrices, vertices, light, overlay);
        }
    }

    @Redirect(method = "renderArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPart;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V", ordinal = 1))
    private void redirectRenderSleeve(ModelPart modelPart, MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        Chams chams = Modules.get().get(Chams.class);

        if (Modules.get().isActive(HandView.class)) return;

        if (chams.isActive() && chams.hand.get()) {
            Color color = chams.handColor.get();
            modelPart.render(matrices, vertices, light, overlay, color.getPacked());
        } else {
            modelPart.render(matrices, vertices, light, overlay);
        }
    }*/
}
