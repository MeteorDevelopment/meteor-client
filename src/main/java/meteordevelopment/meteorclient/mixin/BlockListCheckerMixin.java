/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.network.Address;
import net.minecraft.client.network.BlockListChecker;
import net.minecraft.client.network.ServerAddress;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@NullMarked
@Mixin(BlockListChecker.class)
public interface BlockListCheckerMixin {
    @Inject(method = "create", at = @At("HEAD"), cancellable = true)
    private static void onCreate(CallbackInfoReturnable<BlockListChecker> cir) {
        cir.setReturnValue(new BlockListChecker() {
            @Override
            public boolean isAllowed(Address address) {
                return true;
            }

            @Override
            public boolean isAllowed(ServerAddress address) {
                return true;
            }
        });
    }
}
