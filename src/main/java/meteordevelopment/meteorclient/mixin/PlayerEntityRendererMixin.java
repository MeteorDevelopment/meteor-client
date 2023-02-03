/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.NameProtect;
import meteordevelopment.meteorclient.systems.modules.render.Chams;
import meteordevelopment.meteorclient.systems.modules.render.HandView;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(method = "getTexture(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)Lnet/minecraft/util/Identifier;", at = @At("HEAD"), cancellable = true)
    private void onGetTexture(AbstractClientPlayerEntity abstractClientPlayerEntity, CallbackInfoReturnable<Identifier> info) {
        if (Modules.get().get(NameProtect.class).skinProtect()) info.setReturnValue(DefaultSkinHelper.getTexture());
    }

    @Redirect(method = "renderArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getSkinTexture()Lnet/minecraft/util/Identifier;"))
    private Identifier onGetTexture(AbstractClientPlayerEntity player) {
        return Modules.get().get(NameProtect.class).skinProtect() ? DefaultSkinHelper.getTexture() : player.getSkinTexture();
    }

    @ModifyArgs(method = "renderArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPart;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V", ordinal = 0))
    private void modifyRenderLayer(Args args, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve) {
        Chams chams = Modules.get().get(Chams.class);

        if (chams.isActive() && chams.hand.get()) {
            Identifier texture = chams.handTexture.get() ? player.getSkinTexture() : Chams.BLANK;
            args.set(1, vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(texture)));
        }
    }

    @Redirect(method = "renderArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPart;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V", ordinal = 0))
    private void redirectRenderMain(ModelPart modelPart, MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        Chams chams = Modules.get().get(Chams.class);

        if (chams.isActive() && chams.hand.get()) {
            Color color = chams.handColor.get();
            modelPart.render(matrices, vertices, light, overlay, color.r/255f, color.g/255f, color.b/255f, color.a/255f);
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
            modelPart.render(matrices, vertices, light, overlay, color.r/255f, color.g/255f, color.b/255f, color.a/255f);
        } else {
            modelPart.render(matrices, vertices, light, overlay);
        }
    }
}
