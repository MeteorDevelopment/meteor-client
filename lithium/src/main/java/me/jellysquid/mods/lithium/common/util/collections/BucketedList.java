package me.jellysquid.mods.lithium.common.util.collections;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;

public class BucketedList<T> extends AbstractList<T> {
    final ArrayList<T>[] buckets;
    private int size;

    public BucketedList(int numBuckets) {
        //noinspection unchecked
        this.buckets = new ArrayList[numBuckets];
    }

    public void addToBucket(int bucket, T element) {
        ArrayList<T> bucketList = this.buckets[bucket];
        if (bucketList == null) {
            bucketList = new ArrayList<>();
            this.buckets[bucket] = bucketList;
        }
        bucketList.add(element);
        this.size++;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            int bucketIndex = -1;
            int index;
            int consumed;

            ArrayList<T> bucketList;

            @Override
            public boolean hasNext() {
                return this.consumed < BucketedList.this.size;
            }

            @Override
            public T next() {
                if (this.bucketList == null || this.bucketList.size() <= this.index) {
                    this.bucketIndex++;
                    this.bucketList = BucketedList.this.buckets[this.bucketIndex];
                    this.index = 0;
                    return this.next();
                }
                this.consumed++;
                return this.bucketList.get(this.index++);
            }
        };
    }

    @Override
    public T get(int index) {
        for (ArrayList<T> bucketList : this.buckets) {
            if (bucketList != null) {
                if (index < bucketList.size()) {
                    return bucketList.get(index);
                }
                index -= bucketList.size();
            }
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int size() {
        return this.size;
    }
}
