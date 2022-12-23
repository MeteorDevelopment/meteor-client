/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * {@link ArrayList} wrapper implementation with reusable mutable objects. {@link PooledList#acquire()} instead of {@link List#add(Object)} and {@link PooledList#release(Object)} instead of {@link List#remove(Object)}.
 * @author Crosby 22/12/2022 :prayge:
 */
public class PooledList<T> implements Iterable<T> {
    protected final Pool<T> pool;
    protected final List<T> list = new ArrayList<>();

    public PooledList(Producer<T> producer) {
        pool = new Pool<>(producer);
    }

    // Pool

    /** Get or create an object from the {@link Pool} and add it to the {@link List}. */
    public T acquire() {
        T obj = pool.get();
        list.add(obj);
        return obj;
    }

    /** Return an object to the {@link Pool} and remove it from the {@link List}. */
    public void release(T obj) {
        pool.free(obj);
        list.remove(obj);
    }

    /** Release all in-use objects. */
    public void clear() {
        for (T obj : list) pool.free(obj);
        list.clear();
    }

    /** Release all objects that match the {@link Predicate}. */
    public boolean removeIf(Predicate<? super T> filter) {
        boolean removed = false;
        for (Iterator<T> it = iterator(); it.hasNext();) {
            T obj = it.next();
            if (filter.test(obj)) {
                it.remove();
                removed = true;
            }
        }
        return removed;
    }

    // List

    public int size() {
        return list.size();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public void sort(Comparator<T> comparator) {
        list.sort(comparator);
    }

    public T get(int index) {
        return list.get(index);
    }

    /** Return the first element in the list. */
    public T getFirst() {
        return list.get(0);
    }

    /** Return the last element in the list. */
    public T getLast() {
        return list.get(list.size() - 1);
    }

    public int indexOf(T object) {
        return list.indexOf(object);
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return new PooledListIterator();
    }

    public Stream<T> stream() {
        return list.stream();
    }

    private class PooledListIterator implements Iterator<T> {
        private final Iterator<T> it = list.iterator();
        private T value;

        @Override
        public void remove() {
            pool.free(value);
            it.remove();
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public T next() {
            return value = it.next();
        }
    }
}
