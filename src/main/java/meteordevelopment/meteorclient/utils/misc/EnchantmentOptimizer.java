/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@NullMarked
public class EnchantmentOptimizer {
    private static final int MAXIMUM_MERGE_LEVELS = 39;

    private final Object2IntMap<RegistryEntry<Enchantment>> enchantmentIds;
    private final int[] enchantmentWeights;
    private final Map<ResultKey, Int2ObjectMap<ItemObject>> memoCache;

    public EnchantmentOptimizer(List<RegistryEntry<Enchantment>> enchantments) {
        this.enchantmentIds = new Object2IntOpenHashMap<>();
        this.enchantmentWeights = new int[enchantments.size()];
        this.memoCache = new HashMap<>();

        int id = 0;
        for (RegistryEntry<Enchantment> entry : enchantments) {
            enchantmentIds.put(entry, id);
            int anvilCost = entry.value().getAnvilCost();

            /*
             NOTE: Minecraft's anvil code (AnvilScreenHandler.updateResult) divides the anvil cost by 2
             when merging with enchanted books: "if (bl) { s = Math.max(1, s / 2); }"
             where bl = itemStack3.contains(DataComponentTypes.STORED_ENCHANTMENTS)
             Since we're optimizing book-based enchanting, we divide by 2 to match in-game behavior
            */
            enchantmentWeights[id] = Math.max(1, anvilCost / 2);

            id++;
        }
    }

    public record EnchantmentEntry(RegistryEntry<Enchantment> enchantment, int level) {
    }

    public OptimizationResult optimize(@Nullable Item item, List<EnchantmentEntry> enchants) {
        memoCache.clear();

        // Create enchantment objects
        List<ItemObject> enchantObjs = enchants.stream()
            .map(e -> {
                int id = enchantmentIds.getOrDefault(e.enchantment(), -1);
                if (id == -1) {
                    throw new IllegalArgumentException("Unknown enchantment: " + e.enchantment().getKey().orElseThrow().getValue());
                }
                int value = e.level() * enchantmentWeights[id];
                IntList ids = IntLists.singleton(id);
                ItemObject obj = new ItemObject(ItemType.BOOK, value, ids);
                obj.combination = new Combination(e.enchantment(), e.level(), value);
                return obj;
            })
            .collect(Collectors.toCollection(ArrayList::new));

        // Find most expensive enchant
        int mostExpensiveIdx = findMostExpensive(enchantObjs);

        // Create base item
        ItemObject baseItem;
        if (item == null) { // Book-only mode
            ItemObject expensive = enchantObjs.get(mostExpensiveIdx);
            IntList ids = IntLists.singleton(expensive.enchantIds.getInt(0));
            baseItem = new ItemObject(ItemType.ENCHANTED_BOOK, expensive.value, ids);
            baseItem.combination = expensive.combination;
            enchantObjs.remove(mostExpensiveIdx);
            // Find the next most expensive after removing the first
            mostExpensiveIdx = findMostExpensive(enchantObjs);
        } else {
            baseItem = new ItemObject(ItemType.ITEM, 0, new IntArrayList());
            baseItem.combination = new Combination(item);
        }

        if (enchantObjs.isEmpty()) {
            return new OptimizationResult(baseItem, List.of(), 0, 0);
        }

        // Merge base with most expensive
        ItemObject merged = new MergeEnchants(baseItem, enchantObjs.get(mostExpensiveIdx));
        // Override the left combination with a fresh one that has value=0, priorWork=0
        // This ensures the base item doesn't contribute to the value calculation
        if (item != null) {
            merged.combination.left = new Combination(item);
        }
        enchantObjs.remove(mostExpensiveIdx);

        // Find optimal combination
        List<ItemObject> allObjs = new ArrayList<>(enchantObjs);
        allObjs.add(merged);

        Int2ObjectMap<ItemObject> cheapestItems = cheapestItemsFromList(allObjs);

        // Select cheapest by total XP
        ItemObject cheapest = cheapestItems.values().stream()
            .min(Comparator.comparingInt(a -> a.totalXp))
            .orElseThrow();

        List<Instruction> instructions = getInstructions(cheapest.combination);

        int maxLevels = instructions.stream().mapToInt(i -> i.levels).sum();
        int maxXp = experience(maxLevels);

        return new OptimizationResult(cheapest, instructions, maxLevels, maxXp);
    }

    private int findMostExpensive(List<ItemObject> items) {
        int maxIdx = 0;
        for (int i = 1; i < items.size(); i++) {
            if (items.get(i).value > items.get(maxIdx).value) {
                maxIdx = i;
            }
        }
        return maxIdx;
    }


    private Int2ObjectMap<ItemObject> cheapestItemsFromList(List<ItemObject> items) {
        ResultKey key = ResultKey.fromItems(items);
        Int2ObjectMap<ItemObject> cached = memoCache.get(key);
        if (cached != null) return cached;

        Int2ObjectMap<ItemObject> result = switch (items.size()) {
            case 1 -> Int2ObjectMaps.singleton(items.getFirst().priorWork, items.getFirst());
            case 2 -> {
                ItemObject cheapest = cheapestItemFromItems(items.getFirst(), items.get(1));
                yield Int2ObjectMaps.singleton(cheapest.priorWork, cheapest);
            }
            default -> cheapestItemsFromListN(items, items.size() / 2);
        };

        memoCache.put(key, result);
        return result;
    }

    private ItemObject cheapestItemFromItems(ItemObject left, ItemObject right) {
        if (left.type == ItemType.ITEM) return new MergeEnchants(left, right);
        if (right.type == ItemType.ITEM) return new MergeEnchants(right, left);

        ItemObject normal = null;
        ItemObject reversed = null;

        try {
            normal = new MergeEnchants(left, right);
        } catch (MergeLevelsTooExpensiveException ignored) {
            // Ignore too expensive merges
        }

        try {
            reversed = new MergeEnchants(right, left);
        } catch (MergeLevelsTooExpensiveException ignored) {
            // Ignore too expensive merges
        }

        if (normal == null && reversed == null) {
            throw new IllegalStateException("Both merge attempts were too expensive");
        }

        if (normal == null) return reversed;
        if (reversed == null) return normal;

        // Both merges succeeded - they have same priorWork, so compareCheapest cannot return null
        return compareCheapest(normal, reversed);
    }

    private Int2ObjectMap<ItemObject> cheapestItemsFromListN(List<ItemObject> items, int maxSubcount) {
        Int2ObjectMap<ItemObject> cheapestWork2Item = new Int2ObjectOpenHashMap<>();

        for (int subcount = 1; subcount <= maxSubcount; subcount++) {
            for (List<ItemObject> leftItems : combinations(items, subcount)) {
                List<ItemObject> rightItems = new ArrayList<>(items);
                rightItems.removeAll(leftItems);

                Int2ObjectMap<ItemObject> leftWork2Item = cheapestItemsFromList(leftItems);
                Int2ObjectMap<ItemObject> rightWork2Item = cheapestItemsFromList(rightItems);
                Int2ObjectMap<ItemObject> newWork2Item = cheapestItemsFromDictionaries(leftWork2Item, rightWork2Item);

                for (Int2ObjectMap.Entry<ItemObject> entry : newWork2Item.int2ObjectEntrySet()) {
                    cheapestWork2Item.merge(entry.getIntKey(), entry.getValue(), this::compareCheapest);
                }
            }
        }
        return cheapestWork2Item;
    }

    private ItemObject compareCheapest(ItemObject item1, ItemObject item2) {
        // This method assumes both items have the same priorWork (enforced by callers using work-indexed maps)
        // If they somehow differ, we can't meaningfully compare them
        if (item1.priorWork != item2.priorWork) {
            throw new IllegalStateException("Items must have same priorWork: " + item1.priorWork + " vs " + item2.priorWork);
        }

        // Prefer lower value (fewer enchantment levels)
        if (item1.value != item2.value) return item1.value < item2.value ? item1 : item2;

        // If value is equal, prefer lower total XP cost
        return item1.totalXp <= item2.totalXp ? item1 : item2;
    }

    private Int2ObjectMap<ItemObject> cheapestItemsFromDictionaries(
        Int2ObjectMap<ItemObject> left, Int2ObjectMap<ItemObject> right) {
        Int2ObjectMap<ItemObject> cheapest = new Int2ObjectOpenHashMap<>();

        for (ItemObject leftItem : left.values()) {
            for (ItemObject rightItem : right.values()) {
                try {
                    Int2ObjectMap<ItemObject> newWork2Item = cheapestItemsFromList(List.of(leftItem, rightItem));

                    for (Int2ObjectMap.Entry<ItemObject> entry : newWork2Item.int2ObjectEntrySet()) {
                        cheapest.merge(entry.getIntKey(), entry.getValue(), this::compareCheapest);
                    }
                } catch (MergeLevelsTooExpensiveException ignored) {
                    // Ignore too expensive merges
                }
            }
        }
        return removeExpensiveCandidates(cheapest);
    }

    private Int2ObjectMap<ItemObject> removeExpensiveCandidates(Int2ObjectMap<ItemObject> work2Item) {
        Int2ObjectMap<ItemObject> result = new Int2ObjectOpenHashMap<>();
        int cheapestValue = Integer.MAX_VALUE;

        // Must iterate in sorted order by priorWork (key)
        int[] sortedKeys = work2Item.keySet().toIntArray();
        IntArrays.quickSort(sortedKeys);

        for (int priorWork : sortedKeys) {
            ItemObject item = work2Item.get(priorWork);
            if (item.value < cheapestValue) {
                result.put(priorWork, item);
                cheapestValue = item.value;
            }
        }
        return result;
    }

    private List<Instruction> getInstructions(Combination comb) {
        List<Instruction> instructions = new ArrayList<>();
        extractInstructions(comb, instructions);
        return instructions;
    }

    private void extractInstructions(Combination comb, List<Instruction> instructions) {
        // Recursively extract child instructions first
        if (comb.left != null && comb.left.left != null) extractInstructions(comb.left, instructions);
        if (comb.right != null && comb.right.left != null) extractInstructions(comb.right, instructions);

        if (comb.left != null && comb.right != null) {
            instructions.add(new Instruction(comb.left, comb.right, comb.mergeCost,
                experience(comb.mergeCost), (1 << comb.priorWork) - 1));
        }
    }

    private static int experience(int level) {
        if (level == 0) return 0;
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (int) (2.5 * level * level - 40.5 * level + 360);
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }

    private static <T> List<List<T>> combinations(List<T> set, int k) {
        if (k > set.size() || k <= 0) return List.of();
        if (k == set.size()) return List.of(new ArrayList<>(set));
        if (k == 1) return set.stream().map(List::of).toList();

        List<List<T>> combs = new ArrayList<>();
        for (int i = 0; i < set.size() - k + 1; i++) {
            T head = set.get(i);
            List<List<T>> tailCombs = combinations(set.subList(i + 1, set.size()), k - 1);
            for (List<T> tail : tailCombs) {
                List<T> combination = new ArrayList<>(tail.size() + 1);
                combination.add(head);
                combination.addAll(tail);
                combs.add(combination);
            }
        }
        return combs;
    }

    // Data classes

    public enum ItemType {
        ITEM,           // The actual item being enchanted
        BOOK,           // Enchanted book
        ENCHANTED_BOOK  // Book that's being used as base (for book-only mode)
    }

    public static class ItemObject {
        ItemType type;
        IntList enchantIds;
        Combination combination;
        int priorWork;
        int value;
        int totalXp;

        ItemObject(ItemType type, int value, IntList enchantIds) {
            this.type = type;
            this.value = value;
            this.enchantIds = new IntArrayList(enchantIds);
            this.combination = new Combination();
            this.priorWork = 0;
            this.totalXp = 0;
        }
    }

    public static class MergeEnchants extends ItemObject {
        MergeEnchants(ItemObject left, ItemObject right) {
            super(left.type, left.value + right.value, new IntArrayList());

            int mergeCost = right.value + (1 << left.priorWork) - 1 + (1 << right.priorWork) - 1;
            if (mergeCost > MAXIMUM_MERGE_LEVELS) {
                throw new MergeLevelsTooExpensiveException();
            }

            this.enchantIds.addAll(left.enchantIds);
            this.enchantIds.addAll(right.enchantIds);
            this.priorWork = Math.max(left.priorWork, right.priorWork) + 1;
            this.totalXp = left.totalXp + right.totalXp + experience(mergeCost);
            this.combination = new Combination(left.combination, right.combination, mergeCost, this.priorWork, this.value);
        }
    }

    public static class Combination {
        public @Nullable Combination left;
        public @Nullable Combination right;
        public int mergeCost;                                       // Only valid for merged nodes
        public int priorWork;
        public int value;                                           // Total value (sum of all books)
        public @Nullable Item item;                                 // For base item
        public @Nullable RegistryEntry<Enchantment> enchantment;    // For enchanted book
        public int level;

        Combination() {
            this.left = null;
            this.right = null;
            this.item = null;
            this.enchantment = null;
            this.value = 0;
            this.priorWork = 0;
        }

        // For leaf book nodes
        Combination(RegistryEntry<Enchantment> enchantment, int level, int value) {
            this.left = null;
            this.right = null;
            this.item = null;
            this.enchantment = enchantment;
            this.level = level;
            this.value = value;
            this.priorWork = 0;
        }

        // For base item nodes
        Combination(Item item) {
            this.left = null;
            this.right = null;
            this.item = item;
            this.enchantment = null;
            this.value = 0;
            this.priorWork = 0;
        }

        // For merged nodes
        Combination(Combination left, Combination right, int mergeCost, int priorWork, int value) {
            this.left = left;
            this.right = right;
            this.mergeCost = mergeCost;
            this.priorWork = priorWork;
            this.value = value;
            this.item = null;
            this.enchantment = null;
        }
    }

    public record Instruction(
        Combination left,
        Combination right,
        int levels,
        int xp,
        int priorWorkPenalty
    ) {
    }

    public record OptimizationResult(
        ItemObject finalItem,
        List<Instruction> instructions,
        int totalLevels,
        int totalXp
    ) {
    }

    private record ResultKey(List<ItemHash> hashes) {
        static ResultKey fromItems(List<ItemObject> items) {
            return new ResultKey(items.stream()
                .map(ItemHash::new)
                .sorted()
                .toList());
        }
    }

    private record ItemHash(ItemType itemType, IntList sortedEnchants, int priorWork) implements Comparable<ItemHash> {
        ItemHash(ItemObject item) {
            this(item.type, createSortedEnchants(item.enchantIds), item.priorWork);
        }

        private static IntList createSortedEnchants(IntList enchantIds) {
            IntList sorted = new IntArrayList(enchantIds);
            sorted.sort(IntComparators.NATURAL_COMPARATOR);
            return sorted;
        }

        @Override
        public int compareTo(ItemHash o) {
            int c = itemType.compareTo(o.itemType);
            if (c != 0) return c;
            c = Integer.compare(priorWork, o.priorWork);
            if (c != 0) return c;

            // Compare sizes first
            c = Integer.compare(sortedEnchants.size(), o.sortedEnchants.size());
            if (c != 0) return c;

            // Then element by element
            for (int i = 0; i < sortedEnchants.size(); i++) {
                c = Integer.compare(sortedEnchants.getInt(i), o.sortedEnchants.getInt(i));
                if (c != 0) return c;
            }
            return 0;
        }
    }

    private static class MergeLevelsTooExpensiveException extends RuntimeException {
    }

    // Static factory method to create optimizer from registry
    public static EnchantmentOptimizer create(Registry<Enchantment> registry) {
        // streamEntries returns RegistryEntry.Reference, which extends RegistryEntry
        List<RegistryEntry<Enchantment>> enchantments = new ArrayList<>(registry.streamEntries().toList());
        return new EnchantmentOptimizer(enchantments);
    }
}
