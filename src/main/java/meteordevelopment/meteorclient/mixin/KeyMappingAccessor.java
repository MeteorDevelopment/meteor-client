/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("ALL")
    static Map<String, KeyMapping> getKeysById() {
        return null;
    }

    @Accessor("key")
    InputConstants.Key meteor$getKey();

    @Accessor("clickCount")
    int meteor$getClickCount();

    @Accessor("clickCount")
    void meteor$setClickCount(int timesPressed);

    @Invoker("release")
    void meteor$invokeRelease();
}
