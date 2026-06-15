package meteordevelopment.meteorclient.utils.skyblock.terminal.handlers;

import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.skyblock.terminal.TerminalTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class TerminalHandler {
    protected static final Minecraft mc = Minecraft.getInstance();

    public final TerminalTypes type;
    public final CopyOnWriteArrayList<Integer> solution = new CopyOnWriteArrayList<>();
    public final long timeOpened = System.currentTimeMillis();
    public boolean isClicked = false;
    public int windowCount = 0;

    protected TerminalHandler(TerminalTypes type) {
        this.type = type;
    }

    public void updateSlot(List<ItemStack> items, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= type.windowSize) return;
        if (canSolve(items, slotIndex)) {
            solution.clear();
            solution.addAll(solve(items));
        }
    }

    public void openScreen() {
        isClicked = false;
        windowCount++;
    }

    protected abstract Color renderSlot(int slotIndex);

    public Color getSlotColor(int slotIndex) {
        if (!solution.contains(slotIndex)) return null;
        return renderSlot(slotIndex);
    }

    protected boolean canSolve(List<ItemStack> items, int currentIndex) {
        return currentIndex == type.windowSize - 1;
    }

    public void simulateClick(int slotIndex, int button) {
        int idx = solution.indexOf(slotIndex);
        if (idx != -1) solution.remove(idx);
    }

    public abstract List<Integer> solve(List<ItemStack> items);

    public void click(int slotIndex, int button, boolean simulate) {
        if (mc.screen instanceof AbstractContainerScreen<?> containerScreen) {
            if (simulate) simulateClick(slotIndex, button);
            isClicked = true;
            mc.gameMode.handleContainerInput(
                containerScreen.getMenu().containerId,
                slotIndex,
                2,
                ContainerInput.CLONE,
                mc.player
            );
        }
    }

    public boolean canClick(int slotIndex, int button) {
        return solution.contains(slotIndex);
    }

    public int getClickButton(int slotIndex) {
        return 0;
    }

    public String getSlotText(int slotIndex) {
        return null;
    }
}
