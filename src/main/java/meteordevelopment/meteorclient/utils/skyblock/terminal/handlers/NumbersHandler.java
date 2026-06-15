package meteordevelopment.meteorclient.utils.skyblock.terminal.handlers;

import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.skyblock.terminal.TerminalTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class NumbersHandler extends TerminalHandler {
    public NumbersHandler() {
        super(TerminalTypes.NUMBERS);
    }

    @Override
    public List<Integer> solve(List<ItemStack> items) {
        return items.stream()
            .filter(item -> item.getItem() == Items.RED_STAINED_GLASS_PANE)
            .map(items::indexOf)
            .sorted(java.util.Comparator.comparingInt(i -> items.get(i).getCount()))
            .toList();
    }

    @Override
    public void simulateClick(int slotIndex, int button) {
        if (!solution.isEmpty()) solution.remove(0);
    }

    @Override
    public boolean canClick(int slotIndex, int button) {
        return !solution.isEmpty() && slotIndex == solution.get(0);
    }

    @Override
    protected Color renderSlot(int slotIndex) {
        int idx = solution.indexOf(slotIndex);
        return switch (idx) {
            case 0 -> new Color(0, 255, 0);
            case 1 -> new Color(0, 200, 0);
            case 2 -> new Color(0, 150, 0);
            default -> new Color(0, 0, 0, 0);
        };
    }

    @Override
    public String getSlotText(int slotIndex) {
        int idx = solution.indexOf(slotIndex);
        if (idx >= 0 && idx <= 2) {
            return String.valueOf(Math.abs((solution.size() - 14) - idx) + 1);
        }
        return null;
    }
}
