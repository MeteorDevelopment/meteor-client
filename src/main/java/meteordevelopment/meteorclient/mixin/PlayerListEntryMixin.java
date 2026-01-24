/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.NameProtect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {
    @Shadow
    public abstract GameProfile getProfile();

    @Inject(method = "getSkinTextures", at = @At("HEAD"), cancellable = true)
    private void onGetTexture(CallbackInfoReturnable<SkinTextures> info) {
        NameProtect nameProtect = Modules.get().get(NameProtect.class);
        
        if (nameProtect.nickAll()) {
            // For nick-all, give everyone default skin except yourself if nickOthers is enabled
            if (!nameProtect.nickOthers() || !getProfile().name().equals(MinecraftClient.getInstance().getSession().getUsername())) {
                info.setReturnValue(DefaultSkinHelper.getSkinTextures(getProfile()));
            }
        } else if (getProfile().name().equals(MinecraftClient.getInstance().getSession().getUsername())) {
            // Only apply skin protection to self if nickOthers is false
            if (nameProtect.skinProtect() && !nameProtect.nickOthers()) {
                info.setReturnValue(DefaultSkinHelper.getSkinTextures(getProfile()));
            }
        } else {
            // Apply skin protection to other players if nickOthers is true
            if (nameProtect.skinProtect() && nameProtect.nickOthers()) {
                info.setReturnValue(DefaultSkinHelper.getSkinTextures(getProfile()));
            }
        }
    }

}