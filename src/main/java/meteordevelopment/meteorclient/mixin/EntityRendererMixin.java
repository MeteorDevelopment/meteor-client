/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.Hitboxes;
import meteordevelopment.meteorclient.systems.modules.render.ESP;
import meteordevelopment.meteorclient.systems.modules.render.Fullbright;
import meteordevelopment.meteorclient.systems.modules.render.Nametags;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityHitbox;
import net.minecraft.client.render.entity.state.EntityHitboxAndView;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.LightType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

    @Unique private ESP esp;
    @Unique private NoRender noRender;

    // meteor is already initialised at this point
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(EntityRendererFactory.Context context, CallbackInfo ci) {
        esp = Modules.get().get(ESP.class);
        noRender = Modules.get().get(NoRender.class);
    }

    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void onRenderLabel(T entity, CallbackInfoReturnable<Text> cir) {
        if (noRender.noNametags()) cir.setReturnValue(null);
        if (!(entity instanceof PlayerEntity player)) return;
        if (Modules.get().get(Nametags.class).playerNametags() && !(EntityUtils.getGameMode(player) == null && Modules.get().get(Nametags.class).excludeBots()))
            cir.setReturnValue(null);
    }

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void shouldRender(T entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (noRender.noEntity(entity)) cir.setReturnValue(false);
        if (noRender.noFallingBlocks() && entity instanceof FallingBlockEntity) cir.setReturnValue(false);
    }

    @Inject(method = "canBeCulled", at = @At("HEAD"), cancellable = true)
    void canBeCulled(T entity, CallbackInfoReturnable<Boolean> cir) {
        if (esp.forceRender()) cir.setReturnValue(false);
    }

    @ModifyReturnValue(method = "getSkyLight", at = @At("RETURN"))
    private int onGetSkyLight(int original) {
        return Math.max(Modules.get().get(Fullbright.class).getLuminance(LightType.SKY), original);
    }

    @ModifyReturnValue(method = "getBlockLight", at = @At("RETURN"))
    private int onGetBlockLight(int original) {
        return Math.max(Modules.get().get(Fullbright.class).getLuminance(LightType.BLOCK), original);
    }

    @ModifyExpressionValue(method = "updateRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getLightLevel(Lnet/minecraft/world/LightType;Lnet/minecraft/util/math/BlockPos;)I"))
    private int onGetLightLevel(int original) {
        return Math.max(Modules.get().get(Fullbright.class).getLuminance(LightType.BLOCK), original);
    }

    @Inject(method = "updateRenderState", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/entity/state/EntityRenderState;outlineColor:I", shift = At.Shift.AFTER))
    private void onGetOutlineColor(T entity, S state, float tickProgress, CallbackInfo ci) {
        if (esp.isGlow() && !esp.shouldSkip(entity)) {
            Color color = esp.getColor(entity);

            if (color == null) return;
            state.outlineColor = color.getPacked();
        }
    }

    @Inject(method = "updateShadow(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/state/EntityRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void updateShadow(Entity entity, EntityRenderState renderState, CallbackInfo ci) {
        if (noRender.noDeadEntities() &&
            entity instanceof LivingEntity &&
            renderState instanceof LivingEntityRenderState livingEntityRenderState &&
            livingEntityRenderState.deathTime > 0) {
            ci.cancel();
        }
    }

    // Hitboxes

    @ModifyReturnValue(method = "createHitbox", at = @At("TAIL"))
    private EntityHitboxAndView meteor$createHitbox(EntityHitboxAndView original, T entity, float tickProgress, boolean green) {
        var v = Modules.get().get(Hitboxes.class).getEntityValue(entity);
        if (v == 0) return original;

        var builder = new ImmutableList.Builder<EntityHitbox>();

        for (var hitbox : original.hitboxes()) {
            builder.add(new EntityHitbox(
                hitbox.x0() - v, hitbox.y0() - v, hitbox.z0() - v,
                hitbox.x1() + v, hitbox.y1() + v, hitbox.z1() + v,
                hitbox.offsetX(), hitbox.offsetY(), hitbox.offsetZ(),
                hitbox.red(), hitbox.green(), hitbox.blue()
            ));
        }

        return new EntityHitboxAndView(original.viewX(), original.viewY(), original.viewZ(), builder.build());
    }
}
