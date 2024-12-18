/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.item.CrossbowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CrossbowItem.class)
public interface CrossbowItemAccessor {
    @Invoker("getSpeed")
    static float getSpeed(ChargedProjectilesComponent itemStack) { return 0; }
}
