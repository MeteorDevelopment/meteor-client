package me.jellysquid.mods.lithium.common.state;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;

public class FastImmutableTableCache<R, C, V> {
    private final ObjectOpenCustomHashSet<R[]> rows;
    private final ObjectOpenCustomHashSet<C[]> columns;
    private final ObjectOpenCustomHashSet<V[]> values;

    private final ObjectOpenCustomHashSet<int[]> indices;

    @SuppressWarnings("unchecked")
    public FastImmutableTableCache() {
        this.rows = new ObjectOpenCustomHashSet<>((Hash.Strategy<R[]>) ObjectArrays.HASH_STRATEGY);
        this.columns = new ObjectOpenCustomHashSet<>((Hash.Strategy<C[]>) ObjectArrays.HASH_STRATEGY);
        this.values = new ObjectOpenCustomHashSet<>((Hash.Strategy<V[]>) ObjectArrays.HASH_STRATEGY);

        this.indices = new ObjectOpenCustomHashSet<>(IntArrays.HASH_STRATEGY);
    }

    public synchronized V[] dedupValues(V[] values) {
        return this.values.addOrGet(values);
    }

    public synchronized R[] dedupRows(R[] rows) {
        return this.rows.addOrGet(rows);
    }

    public synchronized C[] dedupColumns(C[] columns) {
        return this.columns.addOrGet(columns);
    }

    public synchronized int[] dedupIndices(int[] ints) {
        return this.indices.addOrGet(ints);
    }
}
