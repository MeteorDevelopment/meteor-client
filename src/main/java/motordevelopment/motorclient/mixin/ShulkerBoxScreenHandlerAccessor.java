/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShulkerBoxScreenHandler.class)
public interface ShulkerBoxScreenHandlerAccessor {
    @Accessor("inventory")
    Inventory motor$getInventory();
}
