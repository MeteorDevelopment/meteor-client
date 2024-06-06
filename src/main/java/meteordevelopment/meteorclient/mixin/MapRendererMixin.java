/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.utils.misc.EmptyIterator;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.map.MapDecoration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MapRenderer.MapTexture.class)
public abstract class MapRendererMixin {
    @ModifyExpressionValue(method = "draw(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ZI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/map/MapState;getDecorations()Ljava/lang/Iterable;"))
    private Iterable<MapDecoration> getIconsProxy(Iterable<MapDecoration> original) {
        return (Modules.get().get(NoRender.class).noMapMarkers()) ? EmptyIterator::new : original;
    }

    @Inject(method = "draw(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ZI)V", at = @At("HEAD"), cancellable = true)
    private void onDraw(MatrixStack matrices, VertexConsumerProvider vertexConsumers, boolean hidePlayerIcons, int light, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noMapContents()) ci.cancel();
    }
}
