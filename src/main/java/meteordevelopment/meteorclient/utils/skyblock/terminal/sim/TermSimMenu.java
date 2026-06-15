package meteordevelopment.meteorclient.utils.skyblock.terminal.sim;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class TermSimMenu extends AbstractContainerMenu {
    private final SimpleContainer container;

    public TermSimMenu(int containerId, int size) {
        super(null, containerId);
        this.container = new SimpleContainer(size);
        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;
            addSlot(new Slot(container, i, 8 + col * 18, 18 + row * 18));
        }
    }

    public void setItem(int slot, ItemStack item) {
        container.setItem(slot, item);
    }

    public ItemStack getItem(int slot) {
        return container.getItem(slot);
    }

    public SimpleContainer getContainer() {
        return container;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
