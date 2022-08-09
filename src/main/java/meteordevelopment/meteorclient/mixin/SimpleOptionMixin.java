/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Function;

@Mixin(SimpleOption.class)
public class SimpleOptionMixin<T> {
    @Shadow @Final @Mutable private SimpleOption.Callbacks<T> callbacks;

    @Unique private SimpleOption.Callbacks<T> aCallbacks;

    @Inject(method = "setValue", at = @At("HEAD"))
    private void onSetValueHead(T value, CallbackInfo info) {
        if (MinecraftClient.getInstance().options == null) return;

        SimpleOption.Callbacks<T> c = getCallbacks();

        if (c != null) {
            aCallbacks = this.callbacks;
            this.callbacks = c;
        }
    }

    @Inject(method = "setValue", at = @At("RETURN"))
    public void onSetValueReturn(T value, CallbackInfo info) {
        if (aCallbacks != null) {
            this.callbacks = aCallbacks;
            aCallbacks = null;
        }
    }

    @SuppressWarnings("unchecked")
    @Inject(method = "getCodec", at = @At("HEAD"), cancellable = true)
    public void onGetCodec(CallbackInfoReturnable<Codec<T>> info) {
        GameOptions options = MinecraftClient.getInstance().options;
        if (options == null) return;

        if ((Object) this == options.getGamma()) {
            info.setReturnValue((Codec<T>) Codec.either(Codec.doubleRange(Double.MIN_VALUE, Double.MAX_VALUE), Codec.BOOL).xmap(either -> either.map(value -> value, value -> value ? 1.0 : 0.0), Either::left));
        }
    }

    @Unique
    public SimpleOption.Callbacks<T> getCallbacks() {
        GameOptions options = MinecraftClient.getInstance().options;

        if ((Object) this == options.getGamma()) {
            return createCallback();
        }
        else if ((Object) this == options.getFov()) {
            return createCallback();
        }

        return null;
    }

    @NotNull
    private SimpleOption.Callbacks<T> createCallback() {
        return new SimpleOption.Callbacks<>() {
            @Override
            public Function<SimpleOption<T>, ClickableWidget> getButtonCreator(SimpleOption.TooltipFactory<T> tooltipFactory, GameOptions gameOptions, int x, int y, int width) {
                return null;
            }

            @Override
            public Optional<T> validate(T value) {
                return Optional.of(value);
            }

            @Override
            public Codec<T> codec() {
                return null;
            }
        };
    }
}
