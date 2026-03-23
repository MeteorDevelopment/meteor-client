/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.mixininterface.IEntityRenderState;
import meteordevelopment.meteorclient.utils.network.Capes;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CapeFeatureRenderer.class)
public abstract class CapeFeatureRendererMixin {
    @ModifyExpressionValue(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/SkinTextures;cape()Lnet/minecraft/util/AssetInfo$TextureAsset;"))
    private AssetInfo.TextureAsset modifyCapeTexture(AssetInfo.TextureAsset original, MatrixStack matrices, OrderedRenderCommandQueue entityRenderCommandQueue, int i, PlayerEntityRenderState state, float f, float g) {
        if (((IEntityRenderState) state).meteor$getEntity() instanceof PlayerEntity player) {
            Identifier id = Capes.get(player);
            return id == null ? original : new AssetInfo.TextureAssetInfo(id, id);
        }

        return original;
    }
}
