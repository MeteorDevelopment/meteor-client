/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Ambience;
import net.minecraft.world.biome.FoliageColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FoliageColors.class)
public abstract class FoliageColorsMixin {

    @ModifyReturnValue(method = "getBirchColor", at = @At("RETURN"))
    private static int onGetBirchColor(int original) {
        return getModifiedColor(original);
    }

    @ModifyReturnValue(method = "getSpruceColor", at = @At("RETURN"))
    private static int onGetSpruceColor(int original) {
        return getModifiedColor(original);
    }

    @ModifyReturnValue(method = "getMangroveColor", at = @At("RETURN"))
    private static int onGetMangroveColor(int original) {
        return getModifiedColor(original);
    }

    @Unique
    private static int getModifiedColor(int original) {
        if (Modules.get() == null) return original;

        Ambience ambience = Modules.get().get(Ambience.class);
        if (ambience.isActive() && ambience.customFoliageColor.get()) {
            return ambience.foliageColor.get().getPacked();
        }

        return original;
    }
}
