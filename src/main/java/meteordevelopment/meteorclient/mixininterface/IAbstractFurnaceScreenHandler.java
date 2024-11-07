/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import net.minecraft.item.ItemStack;

public interface IAbstractFurnaceScreenHandler {
    boolean meteor$isItemSmeltable(ItemStack itemStack);
}
