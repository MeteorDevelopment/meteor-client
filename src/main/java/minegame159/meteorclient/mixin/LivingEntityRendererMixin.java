/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.entity.RenderEntityEvent;
import minegame159.meteorclient.mixininterface.ILivingEntityRenderer;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.render.Freecam;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.player.Rotations;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.scoreboard.AbstractTeam;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin implements ILivingEntityRenderer {
    //Freecam
    @Redirect(method = "hasLabel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getCameraEntity()Lnet/minecraft/entity/Entity;"))
    private Entity hasLabelGetCameraEntityProxy(MinecraftClient mc) {
        if (Modules.get().isActive(Freecam.class)) return null;
        return mc.getCameraEntity();
    }

    //Chams
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void renderPre(LivingEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        RenderEntityEvent.Pre event = MeteorClient.EVENT_BUS.post(RenderEntityEvent.Pre.get(entity));
        if (event.isCancelled()) ci.cancel();
    }

    @Inject(method = "render", at = @At("RETURN"), cancellable = true)
    public void renderPost(LivingEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        RenderEntityEvent.Post event = MeteorClient.EVENT_BUS.post(RenderEntityEvent.Post.get(entity));
        if (event.isCancelled()) ci.cancel();
    }

    //3rd Person Rotation
    @ModifyVariable(method = "render", ordinal = 2, at = @At(value = "STORE", ordinal = 0))
    public float changeYaw(float oldValue, LivingEntity entity) {
        if (entity.equals(Utils.mc.player) && Rotations.rotationTimer < 10) return Rotations.serverYaw;
        return oldValue;
    }

    @ModifyVariable(method = "render", ordinal = 3, at = @At(value = "STORE", ordinal = 0))
    public float changeHeadYaw(float oldValue, LivingEntity entity) {
        if (entity.equals(Utils.mc.player) && Rotations.rotationTimer < 10) return Rotations.serverYaw;
        return oldValue;
    }

    @ModifyVariable(method = "render", ordinal = 5, at = @At(value = "STORE", ordinal = 3))
    public float changePitch(float oldValue, LivingEntity entity) {
        if (entity.equals(Utils.mc.player) && Rotations.rotationTimer < 10) return Rotations.serverPitch;
        return oldValue;
    }

    // Player model rendering in main menu
    @Redirect(method = "hasLabel", at = @At(value = "INVOKE", target = "net.minecraft.client.network.ClientPlayerEntity.getScoreboardTeam()Lnet/minecraft/scoreboard/AbstractTeam;"))
    private AbstractTeam hasLabelClientPlayerEntityGetScoreboardTeamProxy(ClientPlayerEntity player) {
        if (player == null) return null;
        return player.getScoreboardTeam();
    }

    //Model Tweaks
    @Shadow protected EntityModel<Entity> model;
    @Shadow protected abstract void setupTransforms(LivingEntity entity, MatrixStack matrices, float animationProgress, float bodyYaw, float tickDelta);
    @Shadow protected abstract void scale(LivingEntity entity, MatrixStack matrices, float amount);
    @Shadow protected abstract boolean isVisible(LivingEntity entity);
    @Shadow protected abstract float getAnimationCounter(LivingEntity entity, float tickDelta);
    @Shadow @Final protected List<FeatureRenderer<LivingEntity, EntityModel<LivingEntity>>> features;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render(LivingEntity livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        RenderEntityEvent.LiveEntity event = MeteorClient.EVENT_BUS.post(RenderEntityEvent.LiveEntity.get(model, livingEntity, features, f, g, matrixStack, vertexConsumerProvider, i));
        if (event.isCancelled()) ci.cancel();
    }

    @Override
    public void setupTransformsInterface(LivingEntity entity, MatrixStack matrices, float animationProgress, float bodyYaw, float tickDelta) {
        setupTransforms(entity, matrices, animationProgress, bodyYaw, tickDelta);
    }

    @Override
    public void scaleInterface(LivingEntity entity, MatrixStack matrices, float amount) {
        scale(entity, matrices, amount);
    }

    @Override
    public boolean isVisibleInterface(LivingEntity entity) {
        return isVisible(entity);
    }

    @Override
    public float getAnimationCounterInterface(LivingEntity entity, float tickDelta) {
        return getAnimationCounter(entity, tickDelta);
    }
}
