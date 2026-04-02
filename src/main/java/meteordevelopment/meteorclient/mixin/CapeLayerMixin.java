/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.mixininterface.IEntityRenderState;
import meteordevelopment.meteorclient.utils.network.Capes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CapeLayer.class)
public abstract class CapeLayerMixin {
    @ModifyExpressionValue(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/PlayerSkin;cape()Lnet/minecraft/core/ClientAsset$Texture;"))
    private ClientAsset.Texture modifyCapeTexture(ClientAsset.Texture original, PoseStack matrices, SubmitNodeCollector entityRenderCommandQueue, int i, AvatarRenderState state, float f, float g) {
        if (((IEntityRenderState) state).meteor$getEntity() instanceof Player player) {
            Identifier id = Capes.get(player);
            return id == null ? original : new ClientAsset.ResourceTexture(id, id);
        }

        return original;
    }
}
