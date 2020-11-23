/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IAbstractFurnaceScreenHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractFurnaceScreenHandler.class)
public abstract class AbstractFurnaceScreenHandlerMixin implements IAbstractFurnaceScreenHandler {
    @Shadow protected abstract boolean isSmeltable(ItemStack itemStack);

    @Shadow protected abstract boolean isFuel(ItemStack itemStack);

    @Override
    public boolean isSmeltableI(ItemStack itemStack) {
        return isSmeltable(itemStack);
    }

    @Override
    public boolean isFuelI(ItemStack itemStack) {
        return isFuel(itemStack);
    }
}
