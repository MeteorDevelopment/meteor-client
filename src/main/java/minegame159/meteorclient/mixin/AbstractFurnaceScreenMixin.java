package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.misc.AutoSmelter;
import net.minecraft.client.gui.screen.ingame.AbstractFurnaceScreen;
import net.minecraft.client.gui.screen.ingame.ContainerScreen;
import net.minecraft.container.AbstractFurnaceContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceScreen.class)
public abstract class AbstractFurnaceScreenMixin<T extends AbstractFurnaceContainer> extends ContainerScreen<T> {
    public AbstractFurnaceScreenMixin(T container, PlayerInventory playerInventory, Text name) {
        super(container, playerInventory, name);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo info) {
        if (ModuleManager.INSTANCE.isActive(AutoSmelter.class)) ModuleManager.INSTANCE.get(AutoSmelter.class).tick(container);
    }

    @Override
    public void onClose() {
        super.onClose();

        if (ModuleManager.INSTANCE.isActive(AutoSmelter.class)) ModuleManager.INSTANCE.get(AutoSmelter.class).onFurnaceClose();
    }
}
