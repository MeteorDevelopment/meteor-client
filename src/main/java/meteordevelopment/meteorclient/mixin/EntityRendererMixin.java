/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.mixininterface.IEntityRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Fullbright;
import meteordevelopment.meteorclient.systems.modules.render.Nametags;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.render.postprocess.PostProcessShaders;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.LightType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> implements IEntityRenderer {
    @Shadow
    public abstract Identifier getTexture(Entity entity);

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void onRenderLabel(T entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float tickDelta, CallbackInfo ci) {
        if (PostProcessShaders.rendering) ci.cancel();
        if (Modules.get().get(NoRender.class).noNametags()) ci.cancel();
        if (!(entity instanceof PlayerEntity)) return;
        if (Modules.get().get(Nametags.class).playerNametags() && !(EntityUtils.getGameMode((PlayerEntity) entity) == null && Modules.get().get(Nametags.class).excludeBots()))
            ci.cancel();
    }

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void shouldRender(T entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (Modules.get().get(NoRender.class).noEntity(entity)) cir.cancel();
        if (Modules.get().get(NoRender.class).noFallingBlocks() && entity instanceof FallingBlockEntity) cir.cancel();
    }

    @ModifyReturnValue(method = "getSkyLight", at = @At("RETURN"))
    private int onGetSkyLight(int original) {
        return Math.max(Modules.get().get(Fullbright.class).getLuminance(LightType.SKY), original);
    }

    @ModifyReturnValue(method = "getBlockLight", at = @At("RETURN"))
    private int onGetBlockLight(int original) {
        return Math.max(Modules.get().get(Fullbright.class).getLuminance(LightType.BLOCK), original);
    }

    @Override
    public Identifier getTextureInterface(Entity entity) {
        return getTexture(entity);
    }
}
