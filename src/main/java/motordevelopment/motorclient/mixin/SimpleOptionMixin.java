/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import motordevelopment.motorclient.mixininterface.ISimpleOption;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;
import java.util.function.Consumer;

@Mixin(SimpleOption.class)
public abstract class SimpleOptionMixin implements ISimpleOption {
    @Shadow Object value;
    @Shadow @Final private Consumer<Object> changeCallback;

    @Override
    public void motor$set(Object value) {
        if (!MinecraftClient.getInstance().isRunning()) {
            this.value = value;
        } else {
            if (!Objects.equals(this.value, value)) {
                this.value = value;
                this.changeCallback.accept(this.value);
            }
        }
    }
}
