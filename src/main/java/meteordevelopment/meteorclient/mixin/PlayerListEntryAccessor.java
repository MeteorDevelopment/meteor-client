/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Supplier;

@Mixin(PlayerListEntry.class)
public interface PlayerListEntryAccessor {
    @Invoker("texturesSupplier")
    static Supplier<SkinTextures> meteor$texturesSupplier(GameProfile profile) {
        throw new AssertionError();
    }
}
