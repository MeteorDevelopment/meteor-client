/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Chams;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.ClientPlayerLikeEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin<AvatarlikeEntity extends PlayerLikeEntity & ClientPlayerLikeEntity>
    extends LivingEntityRenderer<AvatarlikeEntity, PlayerEntityRenderState, PlayerEntityModel> {
    // Chams

    @Unique
    private Chams chams;

    public PlayerEntityRendererMixin(EntityRendererFactory.Context ctx, PlayerEntityModel model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init$chams(CallbackInfo info) {
        chams = Modules.get().get(Chams.class);
    }

    // Chams - Player scale

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("RETURN"))
    private void updateRenderState$scale(AvatarlikeEntity player, PlayerEntityRenderState state, float f, CallbackInfo ci) {
        if (!chams.isActive() || !chams.players.get()) return;
        if (chams.ignoreSelf.get() && player == mc.player) return;

        float v = chams.playersScale.get().floatValue();
        state.baseScale *= v;

        if (state.nameLabelPos != null)
            ((IVec3d) state.nameLabelPos).meteor$setY(state.nameLabelPos.y + (player.getHeight() * v - player.getHeight()));
    }

    // Chams - Hand Texture

    @ModifyExpressionValue(method = "renderArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderLayer;getEntityTranslucent(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"))
    private RenderLayer renderArm$texture(RenderLayer original, MatrixStack matrixStack, OrderedRenderCommandQueue entityRenderCommandQueue, int light, Identifier skinTexture, ModelPart modelPart, boolean sleeveVisible) {
        if (chams.isActive() && chams.hand.get()) {
            Identifier texture = chams.handTexture.get() ? skinTexture : Chams.BLANK;
            return RenderLayer.getEntityTranslucent(texture);
        }

        return original;
    }

    // Chams - Hand Color

    @WrapWithCondition(method = "renderArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModelPart(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IILnet/minecraft/client/texture/Sprite;)V"))
    private boolean renderArm$color(OrderedRenderCommandQueue instance, ModelPart modelPart, MatrixStack matrixStack, RenderLayer renderLayer, int light, int uv, Sprite sprite) {
        if (chams.isActive() && chams.hand.get()) {
            instance.submitModelPart(modelPart, matrixStack, renderLayer, light, uv, null, chams.handColor.get().getPacked(), null);
            return false;
        }

        return true;
    }

    // Rotations

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("RETURN"))
    private void updateRenderState$rotations(AvatarlikeEntity player, PlayerEntityRenderState state, float f, CallbackInfo info) {
        if (Rotations.rotating && player == mc.player) {
            state.relativeHeadYaw = 0;
            state.bodyYaw = Rotations.serverYaw;
            state.pitch = Rotations.serverPitch;
        }
    }
}
