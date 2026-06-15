package meteordevelopment.meteorclient.utils.skyblock.terminal.handlers;

import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.skyblock.terminal.TerminalTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MelodyHandler extends TerminalHandler {
    private static final Set<Integer> CLICKABLE_SLOTS = Set.of(16, 25, 34, 43);

    public int greenCol = -1;
    public int greenRow = -1;
    public int magentaCol = -1;

    public MelodyHandler() {
        super(TerminalTypes.MELODY);
    }

    @Override
    public void updateSlot(List<ItemStack> items, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= type.windowSize) return;
        solution.clear();
        solution.addAll(solve(items));
    }

    @Override
    public List<Integer> solve(List<ItemStack> items) {
        greenCol = -1;
        greenRow = -1;
        magentaCol = -1;

        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getItem() == Items.LIME_STAINED_GLASS_PANE) {
                greenRow = i / 9;
                greenCol = i % 9;
            }
            if (items.get(i).getItem() == Items.MAGENTA_STAINED_GLASS_PANE) {
                int row = i / 9;
                if (row == 0) {
                    magentaCol = i % 9;
                }
            }
        }

        List<Integer> result = new ArrayList<>();

        if (magentaCol != -1) {
            result.add(magentaCol);
        }

        if (greenRow != -1 && greenCol != -1) {
            result.add(greenRow * 9 + greenCol);
        }

        if (greenCol != -1 && greenCol == magentaCol && greenRow >= 1 && greenRow <= 4) {
            result.add(greenRow * 9 + 7);
        }

        return result;
    }

    @Override
    public boolean canClick(int slotIndex, int button) {
        return CLICKABLE_SLOTS.contains(slotIndex) && solution.contains(slotIndex);
    }

    @Override
    protected Color renderSlot(int slotIndex) {
        int row = slotIndex / 9;
        int col = slotIndex % 9;

        if (row == 0 && col == magentaCol) {
            return new Color(128, 0, 128);
        }
        if (row == greenRow && col == greenCol && greenCol != -1) {
            return new Color(0, 255, 0);
        }
        if (col == 7 && row >= 1 && row <= 4 && solution.contains(slotIndex)) {
            return new Color(0, 255, 0);
        }
        return null;
    }
}
