/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BreakIndicators;
import meteordevelopment.meteorclient.systems.modules.render.ESP;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.systems.modules.world.Ambience;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.client.renderer.state.level.WorldBorderRenderState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {

    @Unique
    private ESP esp;
    @Unique
    private NoRender noRender;

    @Inject(method = "setLevel", at = @At("TAIL"))
    private void onSetLevel(ClientLevel level, CallbackInfo ci) {
        esp = Modules.get().get(ESP.class);
        noRender = Modules.get().get(NoRender.class);
    }

    @WrapWithCondition(
        method = "extract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/WeatherEffectRenderer;extractRenderState(Lnet/minecraft/client/multiplayer/ClientLevel;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/state/level/WeatherRenderState;)V"
        )
    )
    private boolean extractLevel$noWeather(
        WeatherEffectRenderer instance, ClientLevel level, float partialTicks, Vec3 cameraPos, WeatherRenderState renderState
    ) {
        if (noRender.noWeather()) {
            renderState.intensity = 0;
            return false;
        }
        return true;
    }

    @WrapWithCondition(
        method = "extract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/WorldBorderRenderer;extract(Lnet/minecraft/world/level/border/WorldBorder;FLnet/minecraft/world/phys/Vec3;DLnet/minecraft/client/renderer/state/level/WorldBorderRenderState;)V"
        )
    )
    private boolean extractLevel$noWorldBorder(
        net.minecraft.client.renderer.WorldBorderRenderer instance,
        WorldBorder border,
        float deltaPartialTick,
        Vec3 cameraPos,
        double renderDistance,
        WorldBorderRenderState state
    ) {
        if (noRender.noWorldBorder()) {
            state.alpha = 0;
            return false;
        }
        return true;
    }

    @ModifyExpressionValue(
        method = "isEntityVisible",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;isSectionCompiledAndVisible(Lnet/minecraft/core/BlockPos;)Z"
        )
    )
    private boolean isEntityVisible$forceRender(boolean original) {
        if (esp.forceRender()) return true;
        return original;
    }

    @Inject(
        method = "extract",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/state/level/LevelRenderState;cloudColor:I",
            opcode = org.objectweb.asm.Opcodes.PUTFIELD,
            shift = At.Shift.AFTER
        )
    )
    private void extractLevel$cloudColor(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick, CallbackInfo ci) {
        Ambience ambience = Modules.get().get(Ambience.class);
        LevelRenderState levelRenderState = Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState;
        if (ambience.isActive() && ambience.customCloudColor.get()) {
            levelRenderState.cloudColor = ambience.cloudColor.get().getPacked();
        }
    }

    @Inject(method = "extractBlockDestroyAnimation", at = @At("HEAD"), cancellable = true)
    private void onExtractBlockDestroyAnimation(CallbackInfo ci) {
        if (Modules.get().isActive(BreakIndicators.class) || Modules.get().get(NoRender.class).noBlockBreakOverlay()) {
            ci.cancel();
        }
    }
}
