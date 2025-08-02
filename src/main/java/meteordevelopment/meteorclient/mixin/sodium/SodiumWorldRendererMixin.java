/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.sodium;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SodiumWorldRenderer.class)
public class SodiumWorldRendererMixin {
    @ModifyVariable(method = "setupTerrain", at = @At("HEAD"), argsOnly = true)
    private FogParameters modifyFogParameters(FogParameters fogParameters) {
        if (Modules.get() == null) return fogParameters;

        if (Modules.get().get(NoRender.class).noFog()) return FogParameters.NONE;

        return fogParameters;
    }
}
