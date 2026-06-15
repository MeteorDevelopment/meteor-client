package meteordevelopment.meteorclient.utils.skyblock.terminal.handlers;

import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.skyblock.terminal.TerminalTypes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.StainedGlassPaneBlock;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RubixHandler extends TerminalHandler {
    private static final List<DyeColor> RUBIX_COLOR_ORDER = List.of(
        DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.GREEN, DyeColor.BLUE, DyeColor.RED
    );

    private DyeColor lastRubixSolution = null;

    public RubixHandler() {
        super(TerminalTypes.RUBIX);
    }

    @Override
    public List<Integer> solve(List<ItemStack> items) {
        List<ItemStack> panes = items.stream()
            .filter(item -> {
                if (item.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof StainedGlassPaneBlock pane) {
                    return pane.getColor() != DyeColor.BLACK;
                }
                return false;
            })
            .toList();

        List<Integer> best = IntStream.range(0, 100).boxed().collect(Collectors.toList());

        if (lastRubixSolution != null) {
            int lastIndex = RUBIX_COLOR_ORDER.indexOf(lastRubixSolution);
            best = buildSolution(panes, lastIndex, items);
        } else {
            for (int goalIndex = 0; goalIndex < RUBIX_COLOR_ORDER.size(); goalIndex++) {
                List<Integer> candidate = buildSolution(panes, goalIndex, items);
                if (getRealSize(candidate) < getRealSize(best)) {
                    best = candidate;
                    lastRubixSolution = RUBIX_COLOR_ORDER.get(goalIndex);
                }
            }
        }

        return best;
    }

    private List<Integer> buildSolution(List<ItemStack> panes, int goalIndex, List<ItemStack> allItems) {
        List<Integer> result = new ArrayList<>();
        for (ItemStack pane : panes) {
            if (pane.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof StainedGlassPaneBlock paneBlock) {
                int paneIdx = RUBIX_COLOR_ORDER.indexOf(paneBlock.getColor());
                if (paneIdx != goalIndex) {
                    int totalColors = RUBIX_COLOR_ORDER.size();
                    int forward = (goalIndex - paneIdx + totalColors) % totalColors;
                    int reverse = (paneIdx - goalIndex + totalColors) % totalColors;

                    if (forward <= reverse) {
                        for (int i = 0; i < forward; i++) {
                            int idx = allItems.indexOf(pane);
                            if (idx != -1) result.add(idx);
                        }
                    } else {
                        int enc = totalColors - reverse;
                        for (int i = 0; i < enc; i++) {
                            int idx = allItems.indexOf(pane);
                            if (idx != -1) result.add(idx);
                        }
                    }
                }
            }
        }
        return result;
    }

    private int getRealSize(List<Integer> list) {
        int size = 0;
        for (int pane : new HashSet<>(list)) {
            int count = Collections.frequency(list, pane);
            size += count >= 3 ? 5 - count : count;
        }
        return size;
    }

    @Override
    public void simulateClick(int slotIndex, int button) {
        if (!solution.contains(slotIndex)) return;
        int freq = Collections.frequency(solution, slotIndex);
        if (button == 0) {
            if (freq <= 2) {
                solution.remove(Integer.valueOf(slotIndex));
            }
        } else {
            if (freq == 3) {
                solution.add(slotIndex);
            } else if (freq == 4) {
                solution.removeIf(i -> i == slotIndex);
            }
        }
    }

    @Override
    public boolean canClick(int slotIndex, int button) {
        if (!solution.contains(slotIndex)) return false;
        int needed = Collections.frequency(solution, slotIndex);
        return !((needed < 3 && button == 1) || ((needed == 3 || needed == 4) && button != 1));
    }

    @Override
    public int getClickButton(int slotIndex) {
        int freq = Collections.frequency(solution, slotIndex);
        return freq >= 3 ? 1 : 0;
    }

    @Override
    protected Color renderSlot(int slotIndex) {
        int amount = Collections.frequency(solution, slotIndex);
        int clicksRequired = amount < 3 ? amount : amount - 5;
        if (clicksRequired == 0) return null;
        return switch (clicksRequired) {
            case 1 -> new Color(0, 255, 0);
            case 2 -> new Color(0, 200, 0);
            case -1 -> new Color(200, 0, 0);
            default -> new Color(150, 0, 0);
        };
    }

    @Override
    public String getSlotText(int slotIndex) {
        int amount = Collections.frequency(solution, slotIndex);
        int clicksRequired = amount < 3 ? amount : amount - 5;
        if (clicksRequired == 0) return null;
        return String.valueOf(clicksRequired);
    }
}
