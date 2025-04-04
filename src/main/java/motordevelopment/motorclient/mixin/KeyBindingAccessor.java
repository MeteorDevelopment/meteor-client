/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(KeyBinding.class)
public interface KeyBindingAccessor {
    @Accessor("CATEGORY_ORDER_MAP")
    static Map<String, Integer> getCategoryOrderMap() { return null; }

    @Accessor("boundKey")
    InputUtil.Key getKey();

    @Accessor("timesPressed")
    int motor$getTimesPressed();

    @Accessor("timesPressed")
    void motor$setTimesPressed(int timesPressed);
}
