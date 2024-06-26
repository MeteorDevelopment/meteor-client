/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

public class UnorderedArrayList<T> extends AbstractList<T> {
    private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};
    private static final int DEFAULT_CAPACITY = 10;
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private transient T[] items;
    private int size;

    public UnorderedArrayList() {
        items = (T[]) DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
    }

    @Override
    public boolean add(T t) {
        if (size == items.length) grow(size + 1);
        items[size++] = t;
        modCount++;
        return true;
    }

    @Override
    public T set(int index, T element) {
        T old = items[index];
        items[index] = element;
        return old;
    }

    @Override
    public T get(int index) {
        return items[index];
    }

    @Override
    public void clear() {
        modCount++;
        for (int i = 0; i < size; i++) items[i] = null;
        size = 0;
    }

    @Override
    public int indexOf(Object o) {
        for (int i = 0; i < size; i++) {
            if (Objects.equals(items[i], o)) return i;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        final T[] elements = this.items;
        for (int i = size - 1; i >= 0; i--) {
            if (Objects.equals(elements[i], o)) return i;
        }
        return -1;
    }

    @Override
    public boolean remove(Object o) {
        int i = indexOf(o);
        if (i == -1) return false;

        items[i] = null;
        items[i] = items[--size];
        modCount++;
        return true;
    }

    @Override
    public T remove(int index) {
        T old = items[index];
        items[index] = null;
        items[index] = items[--size];
        modCount++;
        return old;
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        int preSize = size;
        int j = 0;

        for (int i = 0; i < size; i++) {
            T item = items[i];

            if (!filter.test(item)) {
                if (j < i) items[j] = item;

                j++;
            }
        }

        size = j;
        return size != preSize;
    }

    @Override
    public int size() {
        return size;
    }

    public void ensureCapacity(int minCapacity) {
        if (minCapacity > items.length
            && !(items == DEFAULTCAPACITY_EMPTY_ELEMENTDATA
            && minCapacity <= DEFAULT_CAPACITY)) {
            modCount++;
            grow(minCapacity);
        }
    }

    private void grow(int minCapacity) {
        items = Arrays.copyOf(items, newCapacity(minCapacity));
    }

    private int newCapacity(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = items.length;
        int newCapacity = oldCapacity + (oldCapacity >> 1);
        if (newCapacity - minCapacity <= 0) {
            if (items == DEFAULTCAPACITY_EMPTY_ELEMENTDATA)
                return Math.max(DEFAULT_CAPACITY, minCapacity);
            if (minCapacity < 0) // overflow
                throw new OutOfMemoryError();
            return minCapacity;
        }
        return (newCapacity - MAX_ARRAY_SIZE <= 0)
            ? newCapacity
            : hugeCapacity(minCapacity);
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE)
                ? Integer.MAX_VALUE
                : MAX_ARRAY_SIZE;
    }
}
