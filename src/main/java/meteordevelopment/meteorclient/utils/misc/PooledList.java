/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public class PooledList<T> implements Iterable<T> {
    protected final Pool<T> pool;
    protected final List<T> list = new ArrayList<>();

    public PooledList(Producer<T> producer) {
        pool = new Pool<>(producer);
    }

    public T get() {
        T obj = pool.get();
        list.add(obj);
        return obj;
    }

    public T peekList() {
        return list.get(0);
    }

    public void clear() {
        for (Iterator<T> it = list.iterator(); it.hasNext(); ) {
            pool.free(it.next());
            it.remove();
        }
        for (T obj : list) free(obj);
        list.clear();
    }

    public void free(T obj) {
        pool.free(obj);
        list.remove(obj);
    }

    /** Required instead of {@link PooledList#free(Object)} when in an iterator loop. {@link PooledList} does not support freeing in foreach loops. */
    public void free(T obj, Iterator<T> iterator) {
        pool.free(obj);
        iterator.remove();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    public void sortList(Comparator<T> comparator) {
        list.sort(comparator);
    }

    public T get(int index) {
        return list.get(index);
    }


    public boolean removeIf(Predicate<? super T> filter) {
        boolean removed = false;
        for (Iterator<T> it = list.iterator(); it.hasNext();) {
            T obj = it.next();
            if (filter.test(obj)) {
                free(obj, it);
                removed = true;
            }
        }
        return removed;
    }
}
