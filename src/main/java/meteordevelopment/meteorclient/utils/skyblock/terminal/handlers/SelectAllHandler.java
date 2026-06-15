package meteordevelopment.meteorclient.utils.skyblock.terminal.handlers;

import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.skyblock.terminal.TerminalTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

public class SelectAllHandler extends TerminalHandler {
    private final Set<String> validPrefixes;

    public SelectAllHandler(DyeColor color) {
        super(TerminalTypes.SELECT);
        this.validPrefixes = switch (color) {
            case BLACK -> Set.of("black", "ink");
            case BLUE -> Set.of("blue", "lapis");
            case BROWN -> Set.of("brown", "cocoa");
            case WHITE -> Set.of("white", "bone", "wool");
            case GREEN -> Set.of("green", "cactus");
            case RED -> Set.of("red", "rose");
            case YELLOW -> Set.of("yellow", "dandelion");
            case LIGHT_GRAY -> Set.of("silver", "light gray");
            default -> Set.of(color.getName().toLowerCase().replace('_', ' '));
        };
    }

    @Override
    public List<Integer> solve(List<ItemStack> items) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            ItemStack item = items.get(i);
            if (item.hasFoil() || item.getItem() == Items.BLACK_STAINED_GLASS_PANE) continue;
            String name = item.getHoverName().getString().toLowerCase();
            for (String prefix : validPrefixes) {
                if (name.startsWith(prefix)) {
                    result.add(i);
                    break;
                }
            }
        }
        return result;
    }

    @Override
    protected Color renderSlot(int slotIndex) {
        return new Color(0, 255, 0);
    }
}
