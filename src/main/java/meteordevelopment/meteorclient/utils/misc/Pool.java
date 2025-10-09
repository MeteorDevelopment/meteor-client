/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;
import java.util.function.Supplier;

public final class Pool<T> {
    private final Queue<T> items = new ArrayDeque<>();
    private final Supplier<T> producer;

    public Pool(Supplier<T> producer) {
        this.producer = producer;
    }

    public synchronized T get() {
        if (!items.isEmpty()) return items.poll();
        return producer.get();
    }

    public synchronized void free(T obj) {
        items.offer(obj);
    }

    public synchronized void freeAll(Iterable<T> objects) {
        if (objects instanceof Collection<T> collection) {
            items.addAll(collection);
        } else {
            objects.forEach(items::add);
        }
    }
}
