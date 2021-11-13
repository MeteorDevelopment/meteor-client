/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Chams;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.AbstractTeam;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.opengl.GL11.*;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @Shadow @Nullable
    protected abstract RenderLayer getRenderLayer(T entity, boolean showBody, boolean translucent, boolean showOutline);

    // Freecam

    @Redirect(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getCameraEntity()Lnet/minecraft/entity/Entity;"))
    private Entity hasLabelGetCameraEntityProxy(MinecraftClient mc) {
        if (Modules.get().isActive(Freecam.class)) return null;
        return mc.getCameraEntity();
    }

    //3rd Person Rotation

    @ModifyVariable(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", ordinal = 2, at = @At(value = "STORE", ordinal = 0))
    public float changeYaw(float oldValue, LivingEntity entity) {
        if (entity.equals(mc.player) && Rotations.rotationTimer < 10) return Rotations.serverYaw;
        return oldValue;
    }

    @ModifyVariable(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", ordinal = 3, at = @At(value = "STORE", ordinal = 0))
    public float changeHeadYaw(float oldValue, LivingEntity entity) {
        if (entity.equals(mc.player) && Rotations.rotationTimer < 10) return Rotations.serverYaw;
        return oldValue;
    }

    @ModifyVariable(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", ordinal = 5, at = @At(value = "STORE", ordinal = 3))
    public float changePitch(float oldValue, LivingEntity entity) {
        if (entity.equals(mc.player) && Rotations.rotationTimer < 10) return Rotations.serverPitch;
        return oldValue;
    }

    // Player model rendering in main menu

    @Redirect(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getScoreboardTeam()Lnet/minecraft/scoreboard/AbstractTeam;"))
    private AbstractTeam hasLabelClientPlayerEntityGetScoreboardTeamProxy(ClientPlayerEntity player) {
        if (player == null) return null;
        return player.getScoreboardTeam();
    }

    // Through walls chams

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"), cancellable = true)
    private void renderHead(T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noDeadEntities() && livingEntity.isDead()) ci.cancel();

        Chams chams = Modules.get().get(Chams.class);

        if (chams.isActive() && chams.shouldRender(livingEntity)) {
            glEnable(GL_POLYGON_OFFSET_FILL);
            glPolygonOffset(1.0f, -1100000.0f);
        }
    }

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("TAIL"))
    private void renderTail(T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        Chams chams = Modules.get().get(Chams.class);

        if (chams.isActive() && chams.shouldRender(livingEntity)) {
            glPolygonOffset(1.0f, 1100000.0f);
            glDisable(GL_POLYGON_OFFSET_FILL);
        }
    }

    // Player chams

    @ModifyArgs(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V"))
    private void modifyScale(Args args, T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        Chams module = Modules.get().get(Chams.class);
        if (!module.isActive() || !module.players.get() || !(livingEntity instanceof PlayerEntity)) return;
        if (module.ignoreSelf.get() && livingEntity == mc.player) return;

        args.set(0, -module.playersScale.get().floatValue());
        args.set(1, -module.playersScale.get().floatValue());
        args.set(2, module.playersScale.get().floatValue());
    }

    @ModifyArgs(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V"))
    private void modifyColor(Args args, T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        Chams module = Modules.get().get(Chams.class);
        if (!module.isActive() || !module.players.get() || !(livingEntity instanceof PlayerEntity)) return;
        if (module.ignoreSelf.get() && livingEntity == mc.player) return;

        Color color = PlayerUtils.getPlayerColor(((PlayerEntity) livingEntity), module.playersColor.get());
        args.set(4, color.r / 255f);
        args.set(5, color.g / 255f);
        args.set(6, color.b / 255f);
        args.set(7, module.playersColor.get().a / 255f);
    }

    @Redirect(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getRenderLayer(Lnet/minecraft/entity/LivingEntity;ZZZ)Lnet/minecraft/client/render/RenderLayer;"))
    private RenderLayer getRenderLayer(LivingEntityRenderer<T, M> livingEntityRenderer, T livingEntity, boolean showBody, boolean translucent, boolean showOutline) {
        Chams module = Modules.get().get(Chams.class);
        if (!module.isActive() || !module.players.get() || !(livingEntity instanceof PlayerEntity) || module.playersTexture.get())
            return getRenderLayer(livingEntity, showBody, translucent, showOutline);
        if (module.ignoreSelf.get() && livingEntity == mc.player)
            return getRenderLayer(livingEntity, showBody, translucent, showOutline);

        return RenderLayer.getItemEntityTranslucentCull(Chams.BLANK);
    }
}
