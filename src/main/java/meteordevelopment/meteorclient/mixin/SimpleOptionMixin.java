/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.ISimpleOption;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;
import java.util.function.Consumer;

@Mixin(OptionInstance.class)
public abstract class SimpleOptionMixin implements ISimpleOption {
    @Shadow Object value;
    @Shadow @Final private Consumer<Object> onValueUpdate;

    @Override
    public void meteor$set(Object value) {
        if (!Minecraft.getInstance().isRunning()) {
            this.value = value;
        } else {
            if (!Objects.equals(this.value, value)) {
                this.value = value;
                this.onValueUpdate.accept(this.value);
            }
        }
    }
}
