/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
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
        for (T obj : list) pool.free(obj);
        list.clear();
    }

    public void free(T obj) {
        pool.free(obj);
        list.remove(obj);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return new PooledListIterator();
    }

    public void sortList(Comparator<T> comparator) {
        list.sort(comparator);
    }

    public T get(int index) {
        return list.get(index);
    }

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
