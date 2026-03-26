/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Map;
import net.minecraft.client.KeyMapping;

@Mixin(KeyMapping.class)
public interface KeyBindingAccessor {
    @Accessor("ALL")
    static Map<String, KeyMapping> getKeysById() { return null; }

    @Accessor("key")
    InputConstants.Key meteor$getKey();

    @Accessor("clickCount")
    int meteor$getTimesPressed();

    @Accessor("clickCount")
    void meteor$setTimesPressed(int timesPressed);

    @Invoker("release")
    void meteor$invokeReset();
}
