package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.MixinValues;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.world.BlockView;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.*;
import java.util.stream.Collectors;

@Mixin(ShulkerBoxBlock.class)
public class ShulkerBoxBlockMixin {
    /**
     * @author MineGame159
     * @reason bc i want
     */
    @Overwrite
    @Environment(EnvType.CLIENT)
    public void buildTooltip(ItemStack stack, BlockView view, List<Text> tooltip, TooltipContext options) {
        CompoundTag compoundTag = stack.getSubTag("BlockEntityTag");
        if (compoundTag != null) {
            if (compoundTag.contains("LootTable", 8)) {
                tooltip.add(new LiteralText("???????"));
            }

            if (compoundTag.contains("Items", 9)) {
                DefaultedList<ItemStack> itemStacks = DefaultedList.ofSize(27, ItemStack.EMPTY);
                Inventories.fromTag(compoundTag, itemStacks);
                int totalItemStacks = 0;
                int displaysItemStacks = 0;

                if (MixinValues.isBetterShulkerTooltip()) {
                    Map<Text, Integer> itemCounts = new HashMap<>();
                    for (ItemStack itemStack : itemStacks) {
                        if (!itemStack.isEmpty()) {
                            Text name = itemStack.getName();
                            int itemCount = itemCounts.computeIfAbsent(name, item -> 0);
                            itemCount += itemStack.getCount();
                            itemCounts.put(name, itemCount);
                        }
                    }

                    totalItemStacks = itemCounts.size();

                    List<Pair<Text, Integer>> items = new ArrayList<>(5);
                    for (int i = 0; i < 5; i++) {
                        if (itemCounts.size() == 0) break;

                        Text bestItem = null;
                        int mostItem = 0;

                        for (Text a : itemCounts.keySet()) {
                            int b = itemCounts.get(a);
                            if (b > mostItem) {
                                mostItem = b;
                                bestItem = a;
                            }
                        }

                        items.add(new Pair<>(bestItem, mostItem));
                        itemCounts.remove(bestItem);
                    }

                    for (Pair<Text, Integer> itemCount : items) {
                        displaysItemStacks++;
                        Text text = itemCount.getLeft().deepCopy();
                        text.append(" x").append(String.valueOf(itemCount.getRight()));
                        tooltip.add(text);
                    }
                } else {
                    for (ItemStack itemStack : itemStacks) {
                        if (!itemStack.isEmpty()) {
                            totalItemStacks++;

                            if (displaysItemStacks <= 4) {
                                displaysItemStacks++;
                                Text text = itemStack.getName().deepCopy();
                                text.append(" x").append(String.valueOf(itemStack.getCount()));
                                tooltip.add(text);
                            }
                        }
                    }
                }

                if (totalItemStacks - displaysItemStacks > 0) {
                    tooltip.add((new TranslatableText("container.shulkerBox.more", new Object[]{totalItemStacks - displaysItemStacks})).formatted(Formatting.ITALIC));
                }

                String key = MeteorClient.INSTANCE.shulkerPeek.getBoundKey().getName();
                key = key.replace("key.keyboard.", "");
                key = key.replace("scancode.", "");
                key = key.replace("key.mouse.", "");
                key = Arrays.stream(key.split("\\.")).map(StringUtils::capitalize).collect(Collectors.joining(" "));
                tooltip.add(new LiteralText(""));
                tooltip.add(new LiteralText("Press " + Formatting.YELLOW + key + Formatting.RESET + " to peek"));
            }
        }
    }
}
