/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import com.google.common.collect.Ordering;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ValueComparableMap<K extends Comparable<K>, V> extends TreeMap<K, V> {
    private final Map<K, V> valueMap;

    public ValueComparableMap(final Ordering<? super V> partialValueOrdering) {
        this(partialValueOrdering, new HashMap<K,V>());
    }

    private ValueComparableMap(Ordering<? super V> partialValueOrdering, HashMap<K, V> valueMap) {
        super(partialValueOrdering.onResultOf(valueMap::get).compound(Comparator.naturalOrder()));
        this.valueMap = valueMap;
    }

    @Override
    public V put(K k, V v) {
        if (valueMap.containsKey(k)) remove(k);
        valueMap.put(k,v);
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
