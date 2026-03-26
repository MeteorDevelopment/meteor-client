/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Chams;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(AvatarRenderer.class)
public abstract class PlayerEntityRendererMixin<AvatarlikeEntity extends Avatar & ClientAvatarEntity>
    extends LivingEntityRenderer<AvatarlikeEntity, AvatarRenderState, PlayerModel> {
    // Chams

    @Unique
    private Chams chams;

    public PlayerEntityRendererMixin(EntityRendererProvider.Context ctx, PlayerModel model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init$chams(CallbackInfo info) {
        chams = Modules.get().get(Chams.class);
    }

    // Chams - Player scale

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("RETURN"))
    private void updateRenderState$scale(AvatarlikeEntity player, AvatarRenderState state, float f, CallbackInfo ci) {
        if (!chams.isActive() || !chams.players.get()) return;
        if (chams.ignoreSelf.get() && player == mc.player) return;

        float v = chams.playersScale.get().floatValue();
        state.scale *= v;

        if (state.nameTagAttachment != null)
            ((IVec3d) state.nameTagAttachment).meteor$setY(state.nameTagAttachment.y + (player.getBbHeight() * v - player.getBbHeight()));
    }

    // Chams - Hand Texture

    @ModifyExpressionValue(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;entityTranslucent(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private RenderType renderArm$texture(RenderType original, PoseStack matrixStack, SubmitNodeCollector entityRenderCommandQueue, int light, Identifier skinTexture, ModelPart modelPart, boolean sleeveVisible) {
        if (chams.isActive() && chams.hand.get()) {
            Identifier texture = chams.handTexture.get() ? skinTexture : Chams.BLANK;
            return RenderTypes.entityTranslucent(texture);
        }

        return original;
    }

    // Chams - Hand Color

    @WrapWithCondition(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModelPart(Lnet/minecraft/client/model/geom/ModelPart;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IILnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"))
    private boolean renderArm$color(SubmitNodeCollector instance, ModelPart modelPart, PoseStack matrixStack, RenderType renderLayer, int light, int uv, TextureAtlasSprite sprite) {
        if (chams.isActive() && chams.hand.get()) {
            instance.submitModelPart(modelPart, matrixStack, renderLayer, light, uv, null, chams.handColor.get().getPacked(), null);
            return false;
        }

        return true;
    }

    // Rotations

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("RETURN"))
    private void updateRenderState$rotations(AvatarlikeEntity player, AvatarRenderState state, float f, CallbackInfo info) {
        if (Rotations.rotating && player == mc.player) {
            state.yRot = 0;
            state.bodyRot = Rotations.serverYaw;
            state.xRot = Rotations.serverPitch;
        }
    }
}
