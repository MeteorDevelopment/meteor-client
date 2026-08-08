/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.cubiomes;

import dev.xpple.cubiomes.CubiomesInit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(CubiomesInit.class)
public class CubiomesInitMixin {
    @ModifyArg(method = "load", at = @At(value = "INVOKE", target = "Ljava/lang/System;load(Ljava/lang/String;)V"))
    private static String replaceLib(String filename) {
        return filename;
        // return "/path/to/libcubiomes.so";
    }
}
