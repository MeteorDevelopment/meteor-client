package me.jellysquid.mods.lithium.mixin.ai.task.replace_streams;

import me.jellysquid.mods.lithium.common.ai.WeightedListIterable;
import net.minecraft.util.collection.WeightedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.List;

@Mixin(WeightedList.class)
public class WeightedListMixin<U> implements WeightedListIterable<U> {
    @Shadow
    @Final
    protected List<WeightedList.Entry<? extends U>> entries;

    @Override
    public Iterator<U> iterator() {
        return new WeightedListIterable.ListIterator<>(this.entries.iterator());
    }
}
