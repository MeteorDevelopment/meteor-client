/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import net.minecraft.client.render.MapRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MapRenderer.class)
public class MapRendererMixin {
    // TODO: Fix
    /*@Inject(method = "draw", at = @At("HEAD"))
    private void onDraw(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int id, MapState state, boolean hidePlayerIcons, int light, CallbackInfo info) {
        if (Modules.get().get(NoRender.class).noMapMarkers()) state.getIcons().clear();
    }*/
}
