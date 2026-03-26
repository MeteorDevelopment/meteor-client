/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;

@Mixin(BuiltInRegistries.class)
public abstract class RegistriesMixin {
    @Redirect(method = "internalRegister(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/WritableRegistry;Lnet/minecraft/core/registries/BuiltInRegistries$RegistryBootstrap;)Lnet/minecraft/core/WritableRegistry;", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/Bootstrap;checkBootstrapCalled(Ljava/util/function/Supplier;)V"))
    private static void ignoreBootstrap(Supplier<String> callerGetter) {
        // nothing
    }
}
