package minegame159.meteorclient.mixininterface;

import net.minecraft.item.ItemStack;

public interface IAbstractFurnaceScreenHandler {
    public boolean isSmeltableI(ItemStack itemStack);

    public boolean isFuelI(ItemStack itemStack);
}
