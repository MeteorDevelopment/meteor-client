/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IAbstractFurnaceScreenHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractFurnaceScreenHandler.class)
public abstract class AbstractFurnaceScreenHandlerMixin implements IAbstractFurnaceScreenHandler {
    @Shadow
    protected abstract boolean isSmeltable(ItemStack itemStack);

    @Override
    public boolean isItemSmeltable(ItemStack itemStack) {
        return isSmeltable(itemStack);
    }
}
