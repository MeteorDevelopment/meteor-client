/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.utils.render.Outlines;
import net.minecraft.client.gl.JsonGlProgram;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(JsonGlProgram.class)
public class JsonGlProgramMixin {
    @ModifyVariable(method = "<init>", at = @At("STORE"))
    private Identifier onInitNewIdentifierModifyVariable(Identifier identifier) {
        if (Outlines.loadingOutlineShader && identifier.getPath().equals("shaders/program/my_entity_outline.json")) {
            return new Identifier("meteor-client", identifier.getPath());
        }

        return identifier;
    }

    @ModifyVariable(method = "getShader", at = @At("STORE"))
    private static Identifier onGetShaderNewIdentifierModifyVariable(Identifier identifier) {
        if (Outlines.loadingOutlineShader && identifier.getPath().equals("shaders/program/my_entity_sobel.fsh")) {
            return new Identifier("meteor-client", identifier.getPath());
        }

        return identifier;
    }
}
