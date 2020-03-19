package minegame159.meteorclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.ContainerScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.container.GenericContainer;
import net.minecraft.container.SlotActionType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GenericContainerScreen.class)
public abstract class GenericContainerScreenMixin extends ContainerScreen<GenericContainer> {
    public GenericContainerScreenMixin(GenericContainer container, PlayerInventory playerInventory, Text name) {
        super(container, playerInventory, name);
    }

    @Override
    protected void init() {
        super.init();

        // Steal
        addButton(new ButtonWidget(x + containerWidth - 50 - 7, y + 3, 50, 12, "Steal", button -> {
            for (int i = 0; i < container.getRows() * 9; i++) {
                MinecraftClient.getInstance().interactionManager.method_2906(container.syncId, i, 0, SlotActionType.QUICK_MOVE, playerInventory.player);
            }

            boolean empty = true;
            for (int i = 0; i < container.getRows() * 9; i++) {
                if (!container.getSlot(i).getStack().isEmpty()) {
                    empty = false;
                    break;
                }
            }

            if (empty) MinecraftClient.getInstance().player.closeContainer();
        }));

        // Dump
        addButton(new ButtonWidget(x + containerWidth - 50 - 7, y + this.containerHeight - 96 - 1, 50, 12, "Dump", button -> {
            for (int i = container.getRows() * 9; i < container.getRows() * 9 + 1 + 3 * 9; i++) {
                MinecraftClient.getInstance().interactionManager.method_2906(container.syncId, i, 0, SlotActionType.QUICK_MOVE, playerInventory.player);
            }
        }));
    }
}
