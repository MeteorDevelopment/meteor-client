package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.misc.AutoBrewer;
import net.minecraft.client.gui.screen.ingame.BrewingStandScreen;
import net.minecraft.client.gui.screen.ingame.ContainerScreen;
import net.minecraft.container.BrewingStandContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BrewingStandScreen.class)
public abstract class BrewingStandScreenMixin extends ContainerScreen<BrewingStandContainer> {
    public BrewingStandScreenMixin(BrewingStandContainer container, PlayerInventory playerInventory, Text name) {
        super(container, playerInventory, name);
    }

    @Override
    public void tick() {
        super.tick();

        if (AutoBrewer.INSTANCE.isActive()) AutoBrewer.INSTANCE.tick(container);
    }

    @Override
    public void onClose() {
        if (AutoBrewer.INSTANCE.isActive()) AutoBrewer.INSTANCE.onBrewingStandClose();

        super.onClose();
    }
}
