/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemFeatureRenderer.class)
public abstract class ItemRendererMixin {
    @ModifyExpressionValue(
        method = "renderItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeStorage$ItemSubmit;foilType()Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;"
        )
    )
    private ItemStackRenderState.FoilType modifyEnchant(ItemStackRenderState.FoilType glint) {
        if (Modules.get().get(NoRender.class).noEnchantGlint()) return ItemStackRenderState.FoilType.NONE;
        return glint;
    }
}
