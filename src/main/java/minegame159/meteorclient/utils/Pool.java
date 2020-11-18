package minegame159.meteorclient.utils;

import java.util.ArrayList;
import java.util.List;

public class Pool<T> {
    private final List<T> items = new ArrayList<>();
    private final Producer<T> producer;

    public Pool(Producer<T> producer) {
        this.producer = producer;
    }

    public T get() {
        if (items.size() > 0) return items.remove(items.size() - 1);
        return producer.create();
    }

    public void free(T obj) {
        items.add(obj);
    }
}
