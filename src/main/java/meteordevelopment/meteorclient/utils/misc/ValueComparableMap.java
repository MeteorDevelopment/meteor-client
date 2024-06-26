/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ValueComparableMap<K extends Comparable<K>, V> extends TreeMap<K, V> {
    private final transient Map<K, V> valueMap;

    public ValueComparableMap(final Comparator<? super V> partialValueComparator) {
        this(partialValueComparator, new HashMap<>());
    }

    private ValueComparableMap(Comparator<? super V> partialValueComparator, HashMap<K, V> valueMap) {
        super((k1, k2) -> {
            int cmp = partialValueComparator.compare(valueMap.get(k1), valueMap.get(k2));
            return cmp != 0 ? cmp : k1.compareTo(k2);
        });

        this.valueMap = valueMap;
    }

    @Override
    public V put(K k, V v) {
        if (valueMap.containsKey(k)) remove(k);
        valueMap.put(k, v);
        return super.put(k, v);
    }

    @Override
    public boolean containsKey(Object key) {
        return valueMap.containsKey(key);
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return containsKey(key) ? get(key) : defaultValue;
    }
}
