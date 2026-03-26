/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IAbstractFurnaceScreenHandler;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractFurnaceMenu.class)
public abstract class AbstractFurnaceScreenHandlerMixin implements IAbstractFurnaceScreenHandler {
    @Shadow
    protected abstract boolean canSmelt(ItemStack itemStack);

    @Override
    public boolean meteor$isItemSmeltable(ItemStack itemStack) {
        return canSmelt(itemStack);
    }
}
