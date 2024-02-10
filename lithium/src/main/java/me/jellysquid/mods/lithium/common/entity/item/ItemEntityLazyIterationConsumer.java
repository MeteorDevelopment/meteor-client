package me.jellysquid.mods.lithium.common.entity.item;

import me.jellysquid.mods.lithium.mixin.util.accessors.ItemEntityAccessor;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.function.LazyIterationConsumer;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * This LazyIterationConsumer collects item entities that the searching item entity can merge with.
 * The merging operation itself is NOT performed, as this would cause ConcurrentModificationException.
 * Instead, the merging operations are simulated to determine a list of item entities the merging operation
 * will be successful with.
 */
public class ItemEntityLazyIterationConsumer implements LazyIterationConsumer<ItemEntity> {
    private final ItemStack stack;
    private final Box box;
    private final Predicate<ItemEntity> predicate;
    private final ArrayList<ItemEntity> mergeEntities;
    private final ItemEntity searchingEntity;
    private int adjustedStackCount;

    public ItemEntityLazyIterationConsumer(ItemEntity searchingEntity, Box box, Predicate<ItemEntity> predicate) {
        this.searchingEntity = searchingEntity;
        this.box = box;
        this.predicate = predicate;
        this.mergeEntities = new ArrayList<>();
        this.stack = this.searchingEntity.getStack();
        this.adjustedStackCount = this.stack.getCount();
    }

    public ArrayList<ItemEntity> getMergeEntities() {
        return this.mergeEntities;
    }

    @Override
    public NextIteration accept(ItemEntity otherItemEntity) {
        if (!this.box.intersects(otherItemEntity.getBoundingBox()) || !this.predicate.test(otherItemEntity)) {
            return NextIteration.CONTINUE;
        }
        //Since we are not creating a copy of the list of entities, we must determine the entities to interact with
        //before starting to modify any item entities, as this would otherwise invalidate the iterator.
        //Dry run the merging logic to determine a small list of entities to merge with:
        int receivedItemCount = predictReceivedItemCount(this.searchingEntity, this.stack, this.adjustedStackCount, otherItemEntity, otherItemEntity.getStack());
        if (receivedItemCount != 0) {
            //The item entity is mergeable, so add it to the list of entities to merge with and adjust the stack count.
            this.mergeEntities.add(otherItemEntity);
            this.adjustedStackCount += receivedItemCount;

            if (this.adjustedStackCount <= 0 || this.adjustedStackCount >= this.stack.getMaxCount()) {
                return NextIteration.ABORT;
            }
        }

        return NextIteration.CONTINUE;
    }

    /**
     * This method is a copies the merging logic from ItemEntity but without applying the changes.
     * Here the number of items transferred between the two item entities is calculated after
     * the two item entities have been determined to be mergeable.
     */
    private static int predictReceivedItemCount(ItemEntity thisEntity, ItemStack thisStack, int adjustedStackCount, ItemEntity otherEntity, ItemStack otherStack) {
        if (Objects.equals(((ItemEntityAccessor) thisEntity).lithium$getOwner(), ((ItemEntityAccessor) otherEntity).lithium$getOwner())
                && ItemEntity.canMerge(thisStack, otherStack)) {
            if (otherStack.getCount() < adjustedStackCount) {
                return getTransferAmount(thisStack.getMaxCount(), adjustedStackCount, otherStack.getCount());
            } else {
                int lostAmount = getTransferAmount(otherStack.getMaxCount(), otherStack.getCount(), adjustedStackCount);
                return -lostAmount;
            }
        }
        return 0;
    }

    /**
     * This method is a copies the merging logic from ItemEntity but without applying the changes.
     * Here the number of items transferred between the two item entities is calculated.
     */
    private static int getTransferAmount(int maxCount, int targetStackCount, int sourceStackCount) {
        return Math.min(Math.min(maxCount, 64) - targetStackCount, sourceStackCount);
    }
}
