package me.jellysquid.mods.lithium.common.entity.item;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.lithium.common.hopper.NotifyingItemStack;
import me.jellysquid.mods.lithium.mixin.util.accessors.ItemStackAccessor;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.function.LazyIterationConsumer;

import java.util.ArrayList;

public class ItemEntityCategorizingList extends ElementCategorizingList<ItemEntity, Item> {

    @SuppressWarnings("unused")
    public static final int DOWNGRADE_THRESHOLD = 10; //TODO implement downgrade
    public static final int UPGRADE_THRESHOLD = 20;


    private final Reference2ReferenceOpenHashMap<ItemEntity, ItemStackSubscriber> subscribers;


    public static ItemEntityCategorizingList wrapDelegate(ArrayList<ItemEntity> delegate) {
        ItemEntityCategorizingList itemEntities = new ItemEntityCategorizingList(delegate);
        itemEntities.initialize();
        return itemEntities;
    }
    private ItemEntityCategorizingList(ArrayList<ItemEntity> delegate) {
        super(delegate);
        this.subscribers = new Reference2ReferenceOpenHashMap<>();
    }

    @Override
    Item getCategory(ItemEntity element) {
        return ((ItemStackAccessor) (Object) element.getStack()).lithium$getItem();
    }

    Item getCategory(ItemStack itemStack) {
        return ((ItemStackAccessor) (Object) itemStack).lithium$getItem();
    }

    @Override
    boolean areSubcategoriesAlwaysEmpty(Item item) {
        return item.getMaxCount() == 1;
    }

    // If there are enough item entities in one category, divide the item entities into 3 buckets:
    // Stacks that are more than 50% full can only merge with stacks that are less than 50% full, etc.
    // Buckets:        A           B            *
    // Content:   "<100% full" "<=50% full"   "any"
    // Search if: "< 50% full" ">=50% full"  otherwise

    @Override
    boolean isSubCategoryA(ItemEntity element) { //<100% full
        ItemStack stack = element.getStack();
        return isSubCategoryA(stack);
    }

    private static boolean isSubCategoryA(ItemStack stack) {
        int count = stack.getCount();
        int maxCount = ((ItemStackAccessor) (Object) stack).lithium$getItem().getMaxCount();
        return isSubCategoryA(count, maxCount);
    }

    private static boolean isSubCategoryA(int count, int maxCount) {
        return count < maxCount;
    }

    @Override
    boolean isSubCategoryB(ItemEntity element) { //<=50% full
        ItemStack stack = element.getStack();
        return isSubCategoryB(stack);
    }

    private static boolean isSubCategoryB(ItemStack stack) {
        int count = stack.getCount();
        int maxCount = ((ItemStackAccessor) (Object) stack).lithium$getItem().getMaxCount();
        return isSubCategoryB(count, maxCount);
    }

    private static boolean isSubCategoryB(int count, int maxCount) {
        return count * 2 <= maxCount;
    }

    public LazyIterationConsumer.NextIteration consumeForEntityStacking(ItemEntity searchingEntity, LazyIterationConsumer<ItemEntity> itemEntityConsumer) {
        Item item = this.getCategory(searchingEntity);
        ItemStack stack = searchingEntity.getStack();
        int count = stack.getCount();
        int maxCount = item.getMaxCount();
        if (count * 2 >= maxCount) { //>=50% full
            return this.consumeCategoryB(itemEntityConsumer, item); // Entities that are <= 50% full.
        } else {
            return this.consumeCategoryA(itemEntityConsumer, item); // Entities that are <100% full
        }
    }

    @Override
    void onElementSubcategorized(ItemEntity element, int index) {
        //Subcategorizing is based on the item stack count. The collection must be updated whenever the stack count changes.
        //Use the item stack subscription system to receive updates:
        ItemStackSubscriber subscriber = new ItemStackSubscriber() {
            @Override
            public void lithium$notifyBeforeCountChange(ItemStack itemStack, int index, int newCount) {
                ItemEntityCategorizingList.this.notifyBeforeCountChange(element, index, newCount);
            }

            @Override
            public void lithium$notifyAfterItemEntityStackSwap(int index, ItemEntity itemEntity, ItemStack oldStack) {
                ItemEntityCategorizingList.this.notifyAfterStackSwap(index, itemEntity, oldStack);
            }
        };
        this.subscribers.put(element, subscriber);
        ((NotifyingItemStack) (Object) element.getStack()).lithium$subscribeWithIndex(subscriber, index);
    }

    private void notifyBeforeCountChange(ItemEntity element, int index, int newCount) {
        //Fix the subcategories the item is added to
        Item item = this.getCategory(element);

        int maxCount = item.getMaxCount();
        boolean categoryA = isSubCategoryA(newCount, maxCount);
        boolean oldCategoryA = this.isSubCategoryA(element);
        boolean categoryB = isSubCategoryB(newCount, maxCount);
        boolean oldCategoryB = this.isSubCategoryB(element);

        this.updateSubcategoryAssignment(item, index, categoryA, oldCategoryA, categoryB, oldCategoryB);
    }

    private void updateSubcategoryAssignment(Item category, int index, boolean categoryA, boolean oldCategoryA, boolean categoryB, boolean oldCategoryB) {
        if (categoryA == oldCategoryA && categoryB == oldCategoryB) {
            return;
        }
        //Remove element from subcategories if needed
        this.removeFromSubCategories(category, index, oldCategoryA && !categoryA, oldCategoryB && !categoryB, false);
        //Add element to subcategories if needed
        this.addToSubCategories(category, index, categoryA && !oldCategoryA, categoryB && !oldCategoryB, false);
    }


    private void notifyAfterStackSwap(int index, ItemEntity element, ItemStack oldStack) {
        ItemStack stack = element.getStack();
        if (stack == oldStack) {
            return;
        }
        //Fix the stack subscription. We can reuse the subscriber object as it only stores a reference to this list and the element.
        ItemStackSubscriber subscriber = this.subscribers.get(element);
        if (subscriber != null) {
            ((NotifyingItemStack) (Object) oldStack).lithium$unsubscribe(subscriber);
            ((NotifyingItemStack) (Object) element.getStack()).lithium$subscribeWithIndex(subscriber, index);
        }

        Item previousCategory = getCategory(oldStack);
        Item category = this.getCategory(element);
        //Fix the indices stored in elementsByType, elementsByTypeA and elementsByTypeB.
        boolean subCategoryA = isSubCategoryA(element);
        boolean subCategoryB = isSubCategoryB(element);

        boolean prevSubCategoryA = isSubCategoryA(oldStack);
        boolean prevSubCategoryB = isSubCategoryB(oldStack);

        this.updateCategoryAndSubcategories(category, previousCategory, index, subCategoryA, prevSubCategoryA, subCategoryB, prevSubCategoryB);

    }

    @Override
    void onElementUnSubcategorized(ItemEntity element) {
        ItemStackSubscriber subscriber = this.subscribers.remove(element);
        ((NotifyingItemStack) (Object) element.getStack()).lithium$unsubscribe(subscriber);
    }

    @Override
    void onCollectionReset() {
        this.onAllElementsUnSubcategorized();
    }

    @Override
    ItemEntity castOrNull(Object element) {
        return element instanceof ItemEntity ? (ItemEntity) element : null;
    }

    private void onAllElementsUnSubcategorized() {
        for (Reference2ReferenceMap.Entry<ItemEntity, ItemStackSubscriber> entry : this.subscribers.reference2ReferenceEntrySet()) {
            ItemEntity itemEntity = entry.getKey();
            ItemStackSubscriber subscriber = entry.getValue();
            ((NotifyingItemStack) (Object) itemEntity.getStack()).lithium$unsubscribe(subscriber);
        }
    }
}
