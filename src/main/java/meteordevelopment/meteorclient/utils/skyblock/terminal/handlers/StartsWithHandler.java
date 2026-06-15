package meteordevelopment.meteorclient.utils.skyblock.terminal.handlers;

import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.skyblock.terminal.TerminalTypes;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

public class StartsWithHandler extends TerminalHandler {
    private final String letter;
    private final Set<Integer> clickedSlots = new HashSet<>();
    private AbstractMap.SimpleEntry<Integer, Integer> clickedSlot = null;

    public StartsWithHandler(String letter) {
        super(TerminalTypes.STARTS_WITH);
        this.letter = letter;
    }

    @Override
    public List<Integer> solve(List<ItemStack> items) {
        if (clickedSlot != null) {
            if (mc.screen instanceof AbstractContainerScreen<?> containerScreen) {
                if (clickedSlot.getKey() != containerScreen.getMenu().containerId) {
                    ItemStack item = items.get(clickedSlot.getValue());
                    if (item.getItem() == Items.NETHER_STAR || item.getItem() == Items.EXPERIENCE_BOTTLE) {
                        clickedSlots.add(clickedSlot.getValue());
                    }
                    clickedSlot = null;
                }
            }
        }

        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            ItemStack item = items.get(i);
            String name = item.getHoverName().getString();
            if (name.toLowerCase().startsWith(letter.toLowerCase())
                && !item.hasFoil()
                && !clickedSlots.contains(i)) {
                result.add(i);
            }
        }
        return result;
    }

    @Override
    public void click(int slotIndex, int button, boolean simulate) {
        if (mc.screen instanceof AbstractContainerScreen<?> containerScreen) {
            if (canClick(slotIndex, button) && clickedSlot == null) {
                clickedSlot = new AbstractMap.SimpleEntry<>(containerScreen.getMenu().containerId, slotIndex);
            }
        }
        super.click(slotIndex, button, simulate);
    }

    @Override
    protected Color renderSlot(int slotIndex) {
        return new Color(0, 255, 0);
    }
}
