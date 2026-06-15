package meteordevelopment.meteorclient.utils.skyblock.terminal.sim;

import meteordevelopment.meteorclient.utils.skyblock.terminal.TerminalTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class SimUtils {
    private static final Random RNG = new Random();

    public static List<ItemStack> generateItems(TerminalTypes type) {
        return switch (type) {
            case PANES -> generatePanes();
            case RUBIX -> generateRubix();
            case NUMBERS -> generateNumbers();
            case STARTS_WITH -> generateStartsWith();
            case SELECT -> generateSelect();
            case MELODY -> generateMelody();
        };
    }

    private static List<ItemStack> generatePanes() {
        List<ItemStack> items = new ArrayList<>(45);
        for (int i = 0; i < 45; i++) {
            items.add(new ItemStack(RNG.nextBoolean() ? Items.RED_STAINED_GLASS_PANE : Items.GRAY_STAINED_GLASS_PANE));
        }
        items.set(RNG.nextInt(45), new ItemStack(Items.RED_STAINED_GLASS_PANE));
        return items;
    }

    private static List<ItemStack> generateRubix() {
        List<ItemStack> items = new ArrayList<>(45);
        DyeColor[] colors = {DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.GREEN, DyeColor.BLUE, DyeColor.RED, DyeColor.BLACK};
        for (int i = 0; i < 45; i++) {
            items.add(new ItemStack(getPaneForDye(colors[RNG.nextInt(colors.length)])));
        }
        return items;
    }

    private static List<ItemStack> generateNumbers() {
        List<ItemStack> items = new ArrayList<>(36);
        for (int i = 0; i < 36; i++) {
            items.add(new ItemStack(Items.GRAY_STAINED_GLASS_PANE));
        }
        int count = 5 + RNG.nextInt(10);
        Set<Integer> placed = new HashSet<>();
        for (int n = 0; n < count; n++) {
            int slot;
            do { slot = RNG.nextInt(36); } while (placed.contains(slot));
            placed.add(slot);
            items.set(slot, new ItemStack(Items.RED_STAINED_GLASS_PANE, n + 1));
        }
        return items;
    }

    private static List<ItemStack> generateStartsWith() {
        List<ItemStack> items = new ArrayList<>(45);
        for (int i = 0; i < 45; i++) {
            items.add(new ItemStack(Items.GRAY_STAINED_GLASS_PANE));
        }
        String[] names = {"Stone", "Spruce Log", "Sandstone", "Smooth Stone", "Soul Sand"};
        for (int i = 0; i < names.length && i < 45; i++) {
            ItemStack stack = new ItemStack(Items.STONE);
            stack.set(DataComponents.ITEM_NAME, Component.literal(names[i]));
            items.set(10 + i, stack);
        }
        return items;
    }

    private static List<ItemStack> generateSelect() {
        List<ItemStack> items = new ArrayList<>(54);
        DyeColor[] colors = DyeColor.values();
        for (int i = 0; i < 54; i++) {
            if (RNG.nextInt(4) == 0) {
                items.add(new ItemStack(getWoolForDye(colors[RNG.nextInt(colors.length)])));
            } else {
                items.add(new ItemStack(Items.BLACK_STAINED_GLASS_PANE));
            }
        }
        return items;
    }

    private static List<ItemStack> generateMelody() {
        List<ItemStack> items = new ArrayList<>(54);
        for (int col = 0; col < 9; col++) {
            items.add(new ItemStack(Items.MAGENTA_STAINED_GLASS_PANE));
        }
        int greenRow = 1 + RNG.nextInt(4);
        int greenCol = 1 + RNG.nextInt(5);
        for (int row = 1; row <= 4; row++) {
            for (int col = 0; col < 9; col++) {
                if (col == 0 || col == 8) {
                    items.add(new ItemStack(Items.GRAY_STAINED_GLASS_PANE));
                } else if (col >= 1 && col <= 5) {
                    if (row == greenRow && col == greenCol) {
                        items.add(new ItemStack(Items.LIME_STAINED_GLASS_PANE));
                    } else {
                        items.add(new ItemStack(Items.GRAY_STAINED_GLASS_PANE));
                    }
                } else if (col == 7) {
                    items.add(new ItemStack(Items.LIME_TERRACOTTA));
                } else {
                    items.add(new ItemStack(Items.GRAY_STAINED_GLASS_PANE));
                }
            }
        }
        for (int col = 0; col < 9; col++) {
            items.add(new ItemStack(Items.MAGENTA_STAINED_GLASS_PANE));
        }
        return items;
    }

    private static Item getPaneForDye(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.WHITE_STAINED_GLASS_PANE;
            case ORANGE -> Items.ORANGE_STAINED_GLASS_PANE;
            case MAGENTA -> Items.MAGENTA_STAINED_GLASS_PANE;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_STAINED_GLASS_PANE;
            case YELLOW -> Items.YELLOW_STAINED_GLASS_PANE;
            case LIME -> Items.LIME_STAINED_GLASS_PANE;
            case PINK -> Items.PINK_STAINED_GLASS_PANE;
            case GRAY -> Items.GRAY_STAINED_GLASS_PANE;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_STAINED_GLASS_PANE;
            case CYAN -> Items.CYAN_STAINED_GLASS_PANE;
            case PURPLE -> Items.PURPLE_STAINED_GLASS_PANE;
            case BLUE -> Items.BLUE_STAINED_GLASS_PANE;
            case BROWN -> Items.BROWN_STAINED_GLASS_PANE;
            case GREEN -> Items.GREEN_STAINED_GLASS_PANE;
            case RED -> Items.RED_STAINED_GLASS_PANE;
            case BLACK -> Items.BLACK_STAINED_GLASS_PANE;
        };
    }

    private static Item getWoolForDye(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.WHITE_WOOL;
            case ORANGE -> Items.ORANGE_WOOL;
            case MAGENTA -> Items.MAGENTA_WOOL;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_WOOL;
            case YELLOW -> Items.YELLOW_WOOL;
            case LIME -> Items.LIME_WOOL;
            case PINK -> Items.PINK_WOOL;
            case GRAY -> Items.GRAY_WOOL;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_WOOL;
            case CYAN -> Items.CYAN_WOOL;
            case PURPLE -> Items.PURPLE_WOOL;
            case BLUE -> Items.BLUE_WOOL;
            case BROWN -> Items.BROWN_WOOL;
            case GREEN -> Items.GREEN_WOOL;
            case RED -> Items.RED_WOOL;
            case BLACK -> Items.BLACK_WOOL;
        };
    }
}
