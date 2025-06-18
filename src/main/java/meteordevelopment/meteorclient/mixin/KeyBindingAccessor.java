/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(KeyBinding.class)
public interface KeyBindingAccessor {
    @Accessor("CATEGORY_ORDER_MAP")
    static Map<String, Integer> getCategoryOrderMap() { return null; }

    @Accessor("KEYS_BY_ID")
    static Map<String, KeyBinding> getKeysById() { return null; }

    @Accessor("boundKey")
    InputUtil.Key getKey();

    @Accessor("timesPressed")
    int meteor$getTimesPressed();

    @Accessor("timesPressed")
    void meteor$setTimesPressed(int timesPressed);

    @Invoker("reset")
    void invokeReset();
}
