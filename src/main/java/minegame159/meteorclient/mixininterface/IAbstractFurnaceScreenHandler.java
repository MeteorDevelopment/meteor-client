package minegame159.meteorclient.mixininterface;

import net.minecraft.item.ItemStack;

public interface IAbstractFurnaceScreenHandler {
    boolean isSmeltableI(ItemStack itemStack);

    boolean isFuelI(ItemStack itemStack);
}
