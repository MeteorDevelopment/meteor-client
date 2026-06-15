package meteordevelopment.meteorclient.utils.skyblock.terminal.handlers;

import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.skyblock.terminal.TerminalTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class PanesHandler extends TerminalHandler {
    public PanesHandler() {
        super(TerminalTypes.PANES);
    }

    @Override
    public List<Integer> solve(List<ItemStack> items) {
        return items.stream()
            .filter(item -> item.getItem() == Items.RED_STAINED_GLASS_PANE)
            .map(items::indexOf)
            .toList();
    }

    @Override
    protected Color renderSlot(int slotIndex) {
        return new Color(0, 255, 0);
    }
}
