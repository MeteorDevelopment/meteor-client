package me.jellysquid.mods.lithium.mixin.util.item_stack_tracking;

import me.jellysquid.mods.lithium.common.entity.item.ItemStackSubscriber;
import me.jellysquid.mods.lithium.common.entity.item.ItemStackSubscriberMulti;
import me.jellysquid.mods.lithium.common.hopper.NotifyingItemStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements NotifyingItemStack {

    @Shadow
    private int count;

    @Unique
    private int myIndex;

    @Unique
    @Nullable
    private ItemStackSubscriber stackChangeSubscriber;


    @ModifyVariable(method = "setCount(I)V", at = @At("HEAD"), argsOnly = true)
    public int updateSubscribers(int count) {
        if (this.stackChangeSubscriber != null && this.count != count) {
            this.stackChangeSubscriber.lithium$notifyBeforeCountChange((ItemStack) (Object) this, this.myIndex, count);
        }
        return count;
    }

    @Override
    public void lithium$subscribe(ItemStackSubscriber subscriber) {
        this.lithium$subscribeWithIndex(subscriber, -1);
    }

    @Override
    public void lithium$subscribeWithIndex(ItemStackSubscriber subscriber, int myIndex) {
        if (this.stackChangeSubscriber != null) {
            this.lithium$registerMultipleSubscribers(subscriber, myIndex);
        } else {
            this.stackChangeSubscriber = subscriber;
            this.myIndex = myIndex;
        }
    }

    @Override
    public void lithium$unsubscribe(ItemStackSubscriber stackList) {
        this.lithium$unsubscribeWithIndex(stackList, -1);
    }

    @Override
    public void lithium$unsubscribeWithIndex(ItemStackSubscriber subscriber, int index) {
        if (this.stackChangeSubscriber == subscriber) {
            this.stackChangeSubscriber = null;
            this.myIndex = -1;
        } else if (this.stackChangeSubscriber instanceof ItemStackSubscriberMulti multiSubscriber) {
            this.stackChangeSubscriber = multiSubscriber.without(subscriber, index);
            this.myIndex = multiSubscriber.getIndex(this.stackChangeSubscriber);
        }  //else: no change, since the inventory wasn't subscribed
    }

    @Unique
    private void lithium$registerMultipleSubscribers(ItemStackSubscriber subscriber, int index) {
        if (this.stackChangeSubscriber instanceof ItemStackSubscriberMulti multiSubscriber) {
            this.stackChangeSubscriber = multiSubscriber.with(subscriber, index);
        } else {
            this.stackChangeSubscriber = new ItemStackSubscriberMulti(this.stackChangeSubscriber, this.myIndex, subscriber, index);
            this.myIndex = -1;
        }
    }

    @Override
    public void lithium$notifyAfterItemEntityStackSwap(ItemEntity itemEntity, ItemStack oldStack) {
        if (this.stackChangeSubscriber != null) {
            this.stackChangeSubscriber.lithium$notifyAfterItemEntityStackSwap(this.myIndex, itemEntity, oldStack);
        }
    }
}
