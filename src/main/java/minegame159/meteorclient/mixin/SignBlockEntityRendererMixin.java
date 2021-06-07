/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SignBlockEntityRenderer.class)
public class SignBlockEntityRendererMixin {
    // TODO: Fix
    /*@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/SignBlockEntity;getTextBeingEditedOnRow(ILjava/util/function/Function;)Lnet/minecraft/text/OrderedText;"))
    private OrderedText onRenderSignBlockEntityGetTextBeingEditedOnRowProxy(SignBlockEntity sign, int row, Function<Text, OrderedText> function) {
        if (Modules.get().get(NoRender.class).noSignText()) return null;

        return sign.getTextBeingEditedOnRow(row, function);
    }*/
}
