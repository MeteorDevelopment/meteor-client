/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import net.minecraft.item.ItemStack;

// Using accessor causes a stackoverflow for some fucking reason
public interface IAbstractFurnaceScreenHandler {
    boolean isItemSmeltable(ItemStack itemStack);
}
