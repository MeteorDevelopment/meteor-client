/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import meteordevelopment.meteorclient.mixininterface.IEntityRenderState;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Chams;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.opengl.GL11C.*;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
    // Freecam

    @ModifyExpressionValue(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getCameraEntity()Lnet/minecraft/entity/Entity;"))
    private Entity hasLabelGetCameraEntityProxy(Entity cameraEntity) {
        return Modules.get().isActive(Freecam.class) ? null : cameraEntity;
    }

    // Player model rendering in main menu

    @ModifyExpressionValue(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getScoreboardTeam()Lnet/minecraft/scoreboard/Team;"))
    private Team hasLabelClientPlayerEntityGetScoreboardTeamProxy(Team team) {
        return (mc.player == null) ? null : team;
    }

    // Chams

    @Unique
    private Chams chams;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init$chams(CallbackInfo info) {
        chams = Modules.get().get(Chams.class);
    }

    // Chams - player color

    @WrapWithCondition(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"))
    private boolean render$render(M instance, MatrixStack matrixStack, VertexConsumer vertexConsumer, int light, int overlay, int color, S state, MatrixStack matrices, VertexConsumerProvider consumers, int i) {
        if (!chams.isActive() || !chams.players.get() || !(((IEntityRenderState) state).meteor$getEntity() instanceof PlayerEntity player)) return true;
        if (chams.ignoreSelf.get() && player == mc.player) return true;

        instance.render(matrixStack, vertexConsumer, light, overlay, PlayerUtils.getPlayerColor(player, chams.playersColor.get()).getPacked());
        return false;
    }

    // Chams - Player texture

    @ModifyReturnValue(method = "getRenderLayer", at = @At("RETURN"))
    private RenderLayer getRenderPlayer(RenderLayer original, S state, boolean showBody, boolean translucent, boolean showOutline) {
        if (!chams.isActive() || !(((IEntityRenderState) state).meteor$getEntity() instanceof PlayerEntity player))
            return original;

        if (!chams.players.get() || chams.playersTexture.get())
            return original;
        if (chams.ignoreSelf.get() && player == mc.player)
            return original;

        return RenderLayer.getItemEntityTranslucentCull(Chams.BLANK);
    }

    // Chams - Through walls

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"), cancellable = true)
    private void render$Head(S state, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo info) {
        Entity entity = ((IEntityRenderState) state).meteor$getEntity();
        if (!(entity instanceof LivingEntity livingEntity)) return;

        if (Modules.get().get(NoRender.class).noDeadEntities() && livingEntity.isDead()) info.cancel();

        if (chams.shouldRender(entity)) {
            glEnable(GL_POLYGON_OFFSET_FILL);
            glPolygonOffset(1.0f, -1100000.0f);
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("TAIL"))
    private void render$Tail(S state, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo info) {
        Entity entity = ((IEntityRenderState) state).meteor$getEntity();
        if (!(entity instanceof LivingEntity livingEntity)) return;

        if (chams.shouldRender(livingEntity)) {
            glPolygonOffset(1.0f, 1100000.0f);
            glDisable(GL_POLYGON_OFFSET_FILL);
        }
    }
}
