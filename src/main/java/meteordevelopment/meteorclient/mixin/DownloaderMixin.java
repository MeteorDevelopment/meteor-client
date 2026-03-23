/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.util.Downloader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.nio.file.Path;
import java.util.UUID;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Taken from <a href="https://github.com/CCBlueX/LiquidBounce">LiquidBounce</a>
 * <p>
 * Copyright (c) 2021 - 2025 CCBlueX
 *
 * @author Izuna
 * @see <a href="https://github.com/CCBlueX/LiquidBounce/blob/nextgen/src/main/java/net/ccbluex/liquidbounce/injection/mixins/minecraft/util/MixinDownloader.java">MixinDownloader.java</a>
 */
@Mixin(Downloader.class)
public class DownloaderMixin {
    @Shadow
    @Final
    private Path directory;

    @ModifyExpressionValue(method = "method_55485", at = @At(value = "INVOKE", target = "Ljava/nio/file/Path;resolve(Ljava/lang/String;)Ljava/nio/file/Path;"))
    private Path hookResolve(Path original, @Local(argsOnly = true) UUID id) {
        UUID accountId = mc.getSession().getUuidOrNull();
        if (accountId == null) {
            MeteorClient.LOG.warn("Failed to change resource pack download directory because the account id is null.");
            return original;
        }

        return directory.resolve(accountId.toString()).resolve(id.toString());
    }
}
