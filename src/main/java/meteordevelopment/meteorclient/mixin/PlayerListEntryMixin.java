/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.NameProtect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {
    @Shadow
    public abstract GameProfile getProfile();

    @Inject(method = "getSkinTexture", at = @At("HEAD"), cancellable = true)
    private void onGetTexture(CallbackInfoReturnable<Identifier> info) {
        if (getProfile().getName().equals(MinecraftClient.getInstance().getSession().getUsername())) {
            if (Modules.get().get(NameProtect.class).skinProtect()) {
                info.setReturnValue(DefaultSkinHelper.getTexture());
            }
        }
    }
}
