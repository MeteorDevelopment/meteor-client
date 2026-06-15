package meteordevelopment.meteorclient.utils.skyblock.terminal.sim;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.skyblock.TerminalSimulator;
import meteordevelopment.meteorclient.utils.skyblock.terminal.TerminalUtils;
import meteordevelopment.meteorclient.utils.skyblock.terminal.handlers.TerminalHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class TermSimGUI extends AbstractContainerScreen<TermSimMenu> {
    private final int windowSize;

    public TermSimGUI(int size, Component title) {
        super(new TermSimMenu(0, size), mc.player.getInventory(), title);
        this.windowSize = size;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        Slot slot = hoveredSlot;
        if (slot == null || !slot.hasItem()) return true;

        int slotId = slot.index;
        int button = click.button();

        TerminalHandler term = TerminalUtils.getCurrentTerm();
        if (term != null) {
            term.click(slotId, button == 0 ? 2 : button, false);
        }

        TerminalSimulator sim = Modules.get().get(TerminalSimulator.class);
        if (sim != null) sim.onSimClick();

        refreshHandler();
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        return false;
    }

    @Override
    public void onClose() {
        TerminalSimulator sim = Modules.get().get(TerminalSimulator.class);
        if (sim != null) sim.onSimScreenClose();
        TerminalUtils.isSimulating = false;
        super.onClose();
    }

    public void fillItems(List<ItemStack> items) {
        TermSimMenu menu = getMenu();
        for (int i = 0; i < items.size() && i < menu.getContainer().getContainerSize(); i++) {
            menu.setItem(i, items.get(i));
        }
    }

    private void refreshHandler() {
        TerminalUtils.populateHandlerFromContainer(this, windowSize);
    }
}
