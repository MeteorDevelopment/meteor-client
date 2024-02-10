package me.jellysquid.mods.lithium.common.util.collections;

import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.util.math.MathHelper;

import java.util.function.Predicate;

/**
 * A lossy hashtable implementation that stores a mapping between an object and a boolean.
 * <p>
 * Any hash collisions will result in an overwrite: this is safe because the correct value can always be recomputed,
 * given that the given operator is deterministic.
 * <p>
 * This implementation is safe to use from multiple threads
 */
public final class Object2BooleanCacheTable<T> {
    private final int mask;

    private final Node<T>[] nodes;

    private final Predicate<T> operator;

    @SuppressWarnings("unchecked")
    public Object2BooleanCacheTable(int capacity, Predicate<T> operator) {
        int capacity1 = MathHelper.smallestEncompassingPowerOfTwo(capacity);
        this.mask = capacity1 - 1;

        this.nodes = (Node<T>[]) new Node[capacity1];

        this.operator = operator;
    }

    private static <T> int hash(T key) {
        return HashCommon.mix(key.hashCode());
    }

    public boolean get(T key) {
        int idx = hash(key) & this.mask;

        Node<T> node = this.nodes[idx];
        if (node != null && key.equals(node.key)) {
            return node.value;
        }

        boolean test = this.operator.test(key);
        this.nodes[idx] = new Node<>(key, test);

        return test;
    }

    static class Node<T> {
        final T key;
        final boolean value;

        Node(T key, boolean value) {
            this.key = key;
            this.value = value;
        }
    }
}
