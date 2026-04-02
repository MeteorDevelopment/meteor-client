/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.ESP;
import meteordevelopment.meteorclient.systems.modules.render.Fullbright;
import meteordevelopment.meteorclient.systems.modules.render.Nametags;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

    @Unique
    private ESP esp;
    @Unique
    private NoRender noRender;

    // meteor is already initialised at this point
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(EntityRendererProvider.Context context, CallbackInfo ci) {
        esp = Modules.get().get(ESP.class);
        noRender = Modules.get().get(NoRender.class);
    }

    @Inject(method = "getNameTag", at = @At("HEAD"), cancellable = true)
    private void onRenderLabel(T entity, CallbackInfoReturnable<Component> cir) {
        if (noRender.noNametags()) cir.setReturnValue(null);
        if (!(entity instanceof Player player)) return;
        if (Modules.get().get(Nametags.class).playerNametags() && !(EntityUtils.getGameMode(player) == null && Modules.get().get(Nametags.class).excludeBots()))
            cir.setReturnValue(null);
    }

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void shouldRender(T entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (noRender.noEntity(entity)) cir.setReturnValue(false);
        if (noRender.noFallingBlocks() && entity instanceof FallingBlockEntity) cir.setReturnValue(false);
    }

    @Inject(method = "affectedByCulling", at = @At("HEAD"), cancellable = true)
    void canBeCulled(T entity, CallbackInfoReturnable<Boolean> cir) {
        if (esp.forceRender()) cir.setReturnValue(false);
    }

    @ModifyReturnValue(method = "getSkyLightLevel", at = @At("RETURN"))
    private int onGetSkyLight(int original) {
        return Math.max(Modules.get().get(Fullbright.class).getLuminance(LightLayer.SKY), original);
    }

    @ModifyReturnValue(method = "getBlockLightLevel", at = @At("RETURN"))
    private int onGetBlockLight(int original) {
        return Math.max(Modules.get().get(Fullbright.class).getLuminance(LightLayer.BLOCK), original);
    }

    @ModifyExpressionValue(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBrightness(Lnet/minecraft/world/level/LightLayer;Lnet/minecraft/core/BlockPos;)I"))
    private int onGetLightLevel(int original) {
        return Math.max(Modules.get().get(Fullbright.class).getLuminance(LightLayer.BLOCK), original);
    }

    @Inject(method = "extractRenderState", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/state/EntityRenderState;outlineColor:I", shift = At.Shift.AFTER, opcode = Opcodes.PUTFIELD))
    private void onGetOutlineColor(T entity, S state, float tickProgress, CallbackInfo ci) {
        if (esp.isGlow() && !esp.shouldSkip(entity)) {
            Color color = esp.getColor(entity);

            if (color == null) return;
            state.outlineColor = color.getPacked();
        }
    }

    @Inject(method = "finalizeRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void updateShadow(Entity entity, EntityRenderState renderState, CallbackInfo ci) {
        if (noRender.noDeadEntities() &&
            entity instanceof LivingEntity &&
            renderState instanceof LivingEntityRenderState livingEntityRenderState &&
            livingEntityRenderState.deathTime > 0) {
            ci.cancel();
        }
    }
}
