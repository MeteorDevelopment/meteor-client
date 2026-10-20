/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mc.MixinTargets;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Supplier;

@Mixin(targets = MixinTargets.REGISTRY_BOOTSTRAP_TARGET)
public abstract class BuiltInRegistriesMixin {
    @WrapWithCondition(method = "internalRegister", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/Bootstrap;checkBootstrapCalled(Ljava/util/function/Supplier;)V"))
    private static boolean skipBootstrapCheck(Supplier<String> location) {
        return false;
    }
}
