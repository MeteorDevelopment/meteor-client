package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IAbstractFurnaceContainer;
import net.minecraft.container.AbstractFurnaceContainer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractFurnaceContainer.class)
public abstract class AbstractFurnaceContainerMixin implements IAbstractFurnaceContainer {
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
