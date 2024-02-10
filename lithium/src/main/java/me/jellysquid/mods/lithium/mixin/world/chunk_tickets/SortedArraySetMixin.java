package me.jellysquid.mods.lithium.mixin.world.chunk_tickets;

import net.minecraft.util.collection.SortedArraySet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;
import java.util.function.Predicate;

@Mixin(SortedArraySet.class)
public abstract class SortedArraySetMixin<T> implements Collection<T> {
    @Shadow
    int size;

    @Shadow
    T[] elements;

    /**
     * Add an optimized implementation of {@link Collection#removeIf(Predicate)} which doesn't attempt to shift
     * the values in the array multiple times with each removal. This also eliminates a number of object allocations
     * and works on the direct backing array, speeding things up a fair chunk.
     */
    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        T[] arr = this.elements;

        int writeLim = this.size;
        int writeIdx = 0;

        for (int readIdx = 0; readIdx < writeLim; readIdx++) {
            T obj = arr[readIdx];

            // If the filter does not pass the object, simply skip over it. The write pointer will
            // not be advanced and the next element to pass will instead take this one's place.
            if (filter.test(obj)) {
                continue;
            }

            // If the read and write pointers are the same, then no removals have occurred so far. This
            // allows us to skip copying unchanged values back into the array.
            if (writeIdx != readIdx) {
                arr[writeIdx] = obj;
            }

            writeIdx++;
        }

        this.size = writeIdx;

        return writeLim != writeIdx;
    }
}
