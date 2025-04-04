/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import net.minecraft.client.util.DefaultSkinHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(DefaultSkinHelper.class)
public abstract class DefaultSkinHelperMixin {
    // Player model rendering in main menu
    @Inject(method = "getSkinTextures(Ljava/util/UUID;)Lnet/minecraft/client/util/SkinTextures;", at = @At("HEAD"), cancellable = true)
    private static void onShouldUseSlimModel(UUID uuid, CallbackInfoReturnable<Boolean> info) {
        if (uuid == null) info.setReturnValue(false);
    }
}
