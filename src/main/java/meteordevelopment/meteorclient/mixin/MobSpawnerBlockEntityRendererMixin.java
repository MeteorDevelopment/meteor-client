/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.MobSpawnerBlockEntityRenderer;
import net.minecraft.client.render.block.entity.state.MobSpawnerBlockEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobSpawnerBlockEntityRenderer.class)
public abstract class MobSpawnerBlockEntityRendererMixin implements BlockEntityRenderer<MobSpawnerBlockEntity, MobSpawnerBlockEntityRenderState> {
    @Inject(method = "renderDisplayEntity", at = @At("HEAD"), cancellable = true)
    private static void onRenderDisplayEntity(CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noMobInSpawner()) ci.cancel();
    }
}
