/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.ISimpleOption;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Consumer;

@Mixin(SimpleOption.class)
public class SimpleOptionMixin implements ISimpleOption {
    @Shadow Object value;
    @Shadow @Final private Consumer<Object> changeCallback;

    @Override
    public void set(Object value) {
        this.value = value;
        changeCallback.accept(value);
    }
}
