package me.jellysquid.mods.lithium.common.entity.item;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;

import java.util.Arrays;

/**
 * Handling shadow items in multiple inventories or item entities
 */
public class ItemStackSubscriberMulti implements ItemStackSubscriber {
    private final ItemStackSubscriber[] subscribers;
    private final int[] indices;

    public ItemStackSubscriberMulti(ItemStackSubscriber subscriber1, int slot1, ItemStackSubscriber subscriber2, int slot2) {
        if (subscriber1 == subscriber2 && slot1 == slot2) {
            throw new IllegalArgumentException("Cannot create a multi-subscriber with two identical subscribers");
        }
        this.subscribers = new ItemStackSubscriber[]{subscriber1, subscriber2};
        this.indices = new int[]{slot1, slot2};
    }
    private ItemStackSubscriberMulti(ItemStackSubscriber[] subscribers, int[] indices) {
        if (subscribers.length <= 1) {
            throw new IllegalArgumentException("Cannot create a multi-subscriber with only one subscriber");
        }
        if (subscribers.length != indices.length) {
            throw new IllegalArgumentException("Cannot create a multi-subscriber with different subscriber and slot lengths");
        }

        if (Arrays.asList(subscribers).contains(null)) {
            throw new IllegalArgumentException("Cannot create a multi-subscriber with null subscribers");
        }

        this.subscribers = subscribers;
        this.indices = indices;
    }

    public ItemStackSubscriberMulti with(ItemStackSubscriber subscriber, int index) {
        ItemStackSubscriber[] itemStackSubscribers = this.subscribers;
        for (int i = 0; i < itemStackSubscribers.length; i++) {
            ItemStackSubscriber sub = itemStackSubscribers[i];
            if (sub == subscriber && this.indices[i] == index) {
                return this;
            }
        }

        ItemStackSubscriber[] newSubscribers = new ItemStackSubscriber[this.subscribers.length + 1];
        int[] newSlots = new int[this.indices.length + 1];
        System.arraycopy(this.subscribers, 0, newSubscribers, 0, this.subscribers.length);
        System.arraycopy(this.indices, 0, newSlots, 0, this.indices.length);
        newSubscribers[this.subscribers.length] = subscriber;
        newSlots[this.indices.length] = index;
        return new ItemStackSubscriberMulti(newSubscribers, newSlots);
    }

    public ItemStackSubscriber without(ItemStackSubscriber subscriber, int index) {
        ItemStackSubscriber[] newSubscribers = new ItemStackSubscriber[this.subscribers.length - 1];
        int[] newSlots = new int[this.indices.length - 1];
        int i = 0;

        for (int j = 0; j < this.subscribers.length; j++) {
            if (this.subscribers[j] != subscriber || (index != -1 && this.indices[j] != index)) {
                if (i == newSubscribers.length) {
                    return this; // not in this multi-subscriber, no change
                }
                newSubscribers[i] = this.subscribers[j];
                newSlots[i] = this.indices[j];
                i++;
            }
        }

        if (i < newSubscribers.length) {
            newSubscribers = Arrays.copyOf(newSubscribers, i);
            newSlots = Arrays.copyOf(newSlots, i);
        }

        return newSubscribers.length == 1 ? newSubscribers[0] : new ItemStackSubscriberMulti(newSubscribers, newSlots);
    }

    public int getIndex(ItemStackSubscriber subscriber) {
        for (int i = 0; i < this.subscribers.length; i++) {
            if (this.subscribers[i] == subscriber) {
                return this.indices[i];
            }
        }
        return -1;
    }

    @Override
    public void lithium$notifyBeforeCountChange(ItemStack itemStack, int index, int newCount) {
        ItemStackSubscriber[] itemStackSubscribers = this.subscribers;
        for (int i = 0; i < itemStackSubscribers.length; i++) {
            ItemStackSubscriber subscriber = itemStackSubscribers[i];
            subscriber.lithium$notifyBeforeCountChange(itemStack, this.indices[i], newCount);
        }
    }

    @Override
    public void lithium$notifyAfterItemEntityStackSwap(int index, ItemEntity itemEntity, ItemStack oldStack) {
        ItemStackSubscriber[] itemStackSubscribers = this.subscribers;
        for (int i = 0; i < itemStackSubscribers.length; i++) {
            ItemStackSubscriber subscriber = itemStackSubscribers[i];
            subscriber.lithium$notifyAfterItemEntityStackSwap(this.indices[i], itemEntity, oldStack);
        }
    }
}
