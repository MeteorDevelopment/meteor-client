package minegame159.meteorclient.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractFurnaceScreenHandler.class)
public interface AbstractFurnaceScreenHandlerAccessor {
    @Invoker("isSmeltable")
    boolean isSmeltable(ItemStack itemStack);

    @Invoker("isFuel")
    boolean isFuel(ItemStack itemStack);
}
