/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemFeatureRenderer.class)
public abstract class ItemRendererMixin {
    @ModifyVariable(
        method = "renderItem",
        at = @At("STORE"),
        name = "foilType"
    )
    private ItemStackRenderState.FoilType modifyEnchant(ItemStackRenderState.FoilType foilType) {
        if (Modules.get().get(NoRender.class).noEnchantGlint()) {
            return ItemStackRenderState.FoilType.NONE;
        }

        return foilType;
    }
}
