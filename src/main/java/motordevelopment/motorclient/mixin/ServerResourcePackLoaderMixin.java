/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import motordevelopment.motorclient.systems.modules.Modules;
import motordevelopment.motorclient.systems.modules.misc.ServerSpoof;
import net.minecraft.client.resource.server.ServerResourcePackLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerResourcePackLoader.class)
public class ServerResourcePackLoaderMixin {
    @Inject(method = "onReloadSuccess", at = @At("TAIL"))
    private void removeInactivePacksTail(CallbackInfo ci) {
        Modules.get().get(ServerSpoof.class).silentAcceptResourcePack = false;
    }
}
