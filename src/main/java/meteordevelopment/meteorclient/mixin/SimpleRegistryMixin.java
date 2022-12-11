/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.registry.SimpleRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Mixin(SimpleRegistry.class)
public class SimpleRegistryMixin<T> {
    @Redirect(method = "<init>(Lnet/minecraft/registry/RegistryKey;Lcom/mojang/serialization/Lifecycle;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/Bootstrap;ensureBootstrapped(Ljava/util/function/Supplier;)V"))
    private void idk(Supplier<String> callerGetter) {
        // TODO: Probably extremely retarded but seems to work
       // nothing :trolla:
    }
}
