/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.serialization.Codec;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleOption.class)
public class SimpleOptionMixin<T> {
    @Shadow T value;

    @Inject(method = "setValue", at = @At("HEAD"), cancellable = true)
    private void onSetValueHead(T value, CallbackInfo info) {
        GameOptions options = MinecraftClient.getInstance().options;
        if (options == null) return;

        if ((Object) this == options.getGamma() || (Object) this == options.getFov()) {
            this.value = value;
            info.cancel();
        }
    }

    @SuppressWarnings("unchecked")
    @Inject(method = "getCodec", at = @At("HEAD"), cancellable = true)
    public void onGetCodec(CallbackInfoReturnable<Codec<T>> info) {
        GameOptions options = MinecraftClient.getInstance().options;
        if (options == null) return;

        if ((Object) this == options.getGamma()) {
            info.setReturnValue((Codec<T>) Codec.DOUBLE);
        }
    }
}
