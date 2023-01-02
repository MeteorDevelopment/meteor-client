/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = YggdrasilMinecraftSessionService.class, remap = false)
public class YggdrasilMinecraftSessionServiceMixin {
    @Inject(method = "isAllowedTextureDomain", at = @At(value = "INVOKE", target = "Ljava/net/URI;getHost()Ljava/lang/String;"), cancellable = true)
    private static void modifyAllowedDomain(CallbackInfoReturnable<Boolean> info) {
        info.setReturnValue(true);
    }
}
