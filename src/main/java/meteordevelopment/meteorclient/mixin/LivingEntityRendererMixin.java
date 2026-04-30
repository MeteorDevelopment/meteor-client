/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.mixininterface.IEntityRenderState;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Chams;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
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

    @ModifyExpressionValue(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getCameraEntity()Lnet/minecraft/world/entity/Entity;"))
    private Entity hasLabelGetCameraEntityProxy(Entity cameraEntity) {
        return Modules.get().isActive(Freecam.class) ? null : cameraEntity;
    }

    // Player model rendering in main menu

    @ModifyExpressionValue(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getTeam()Lnet/minecraft/world/scores/PlayerTeam;"))
    private PlayerTeam hasLabelClientPlayerEntityGetScoreboardTeamProxy(PlayerTeam team) {
        return (mc.player == null) ? null : team;
    }

    // Chams

    @Unique
    private Chams chams;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init$chams(CallbackInfo ci) {
        chams = Modules.get().get(Chams.class);
    }

    // Chams - player color

    @WrapWithCondition(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
    private <TState> boolean render$render(SubmitNodeCollector instance, Model<? super TState> model, TState state, PoseStack matrixStack, RenderType renderLayer, int light, int overlay, int mixColor, TextureAtlasSprite sprite, int outlineColor, ModelFeatureRenderer.CrumblingOverlay crumblingOverlayCommand) {
        if (!chams.isActive() || !chams.players.get() || !(((IEntityRenderState) state).meteor$getEntity() instanceof Player player))
            return true;
        if (chams.ignoreSelf.get() && player == mc.player) return true;

        instance.submitModel(model, state, matrixStack, renderLayer, light, overlay, PlayerUtils.getPlayerColor(player, chams.playersColor.get()).getPacked(), sprite, outlineColor, null);
        return false;
    }

    // Chams - Player texture

    @ModifyReturnValue(method = "getRenderType", at = @At("RETURN"))
    private RenderType getRenderPlayer(RenderType original, S state, boolean isBodyVisible, boolean forceTransparent, boolean appearGlowing) {
        if (!chams.isActive() || !(((IEntityRenderState) state).meteor$getEntity() instanceof Player player))
            return original;

        if (!chams.players.get() || chams.playersTexture.get())
            return original;
        if (chams.ignoreSelf.get() && player == mc.player)
            return original;

        return RenderTypes.itemTranslucent(Chams.BLANK);
    }

    // Chams - Through walls

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void render$Head(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        Entity entity = ((IEntityRenderState) state).meteor$getEntity();
        if (!(entity instanceof LivingEntity livingEntity)) return;

        if (Modules.get().get(NoRender.class).noDeadEntities() && livingEntity.isDeadOrDying()) ci.cancel();

        if (chams.shouldRender(entity)) {
            glEnable(GL_POLYGON_OFFSET_FILL);
            glPolygonOffset(1.0f, -1100000.0f);
        }
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("TAIL"))
    private void render$Tail(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        Entity entity = ((IEntityRenderState) state).meteor$getEntity();
        if (!(entity instanceof LivingEntity livingEntity)) return;

        if (chams.shouldRender(livingEntity)) {
            glPolygonOffset(1.0f, 1100000.0f);
            glDisable(GL_POLYGON_OFFSET_FILL);
        }
    }
}
