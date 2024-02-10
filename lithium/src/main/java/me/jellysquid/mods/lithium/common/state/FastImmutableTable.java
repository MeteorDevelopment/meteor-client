package me.jellysquid.mods.lithium.common.state;

import com.google.common.collect.Table;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static it.unimi.dsi.fastutil.HashCommon.arraySize;

public class FastImmutableTable<R, C, V> implements Table<R, C, V> {
    private R[] rowKeys;
    private int[] rowIndices;
    private final int rowMask;

    private C[] colKeys;
    private int[] colIndices;
    private final int colMask;
    private final int colCount;

    private V[] values;
    private final int size;

    @SuppressWarnings("unchecked")
    public FastImmutableTable(Table<R, C, V> table, FastImmutableTableCache<R, C, V> cache) {
        if (cache == null) {
            throw new IllegalArgumentException("Cache must not be null");
        }

        float loadFactor = Hash.DEFAULT_LOAD_FACTOR;

        Set<R> rowKeySet = table.rowKeySet();
        Set<C> colKeySet = table.columnKeySet();

        int rowCount = rowKeySet.size();
        this.colCount = colKeySet.size();

        int rowN = arraySize(rowCount, loadFactor);
        int colN = arraySize(this.colCount, loadFactor);

        this.rowMask = rowN - 1;
        this.rowKeys = (R[]) new Object[rowN];
        this.rowIndices = new int[rowN];

        this.colMask = colN - 1;
        this.colKeys = (C[]) new Object[colN];
        this.colIndices = new int[colN];

        this.createIndex(this.colKeys, this.colIndices, this.colMask, colKeySet);
        this.createIndex(this.rowKeys, this.rowIndices, this.rowMask, rowKeySet);

        this.values = (V[]) new Object[rowCount * this.colCount];

        for (Cell<R, C, V> cell : table.cellSet()) {
            int colIdx = this.getIndex(this.colKeys, this.colIndices, this.colMask, cell.getColumnKey());
            int rowIdx = this.getIndex(this.rowKeys, this.rowIndices, this.rowMask, cell.getRowKey());

            if (colIdx < 0 || rowIdx < 0) {
                throw new IllegalStateException("Missing index for " + cell);
            }

            this.values[this.colCount * rowIdx + colIdx] = cell.getValue();
        }

        this.size = table.size();

        this.rowKeys = cache.dedupRows(this.rowKeys);
        this.rowIndices = cache.dedupIndices(this.rowIndices);

        this.colIndices = cache.dedupIndices(this.colIndices);
        this.colKeys = cache.dedupColumns(this.colKeys);

        this.values = cache.dedupValues(this.values);
    }

    private <T> void createIndex(T[] keys, int[] indices, int mask, Collection<T> iterable) {
        int index = 0;

        for (T obj : iterable) {
            int i = this.find(keys, mask, obj);

            if (i < 0) {
                int pos = -i - 1;

                keys[pos] = obj;
                indices[pos] = index++;
            }
        }
    }

    private <T> int getIndex(T[] keys, int[] indices, int mask, T key) {
        int pos = this.find(keys, mask, key);

        if (pos < 0) {
            return -1;
        }

        return indices[pos];
    }

    @Override
    public boolean contains(Object rowKey, Object columnKey) {
        return this.get(rowKey, columnKey) != null;
    }

    @Override
    public boolean containsRow(Object rowKey) {
        return this.find(this.rowKeys, this.rowMask, rowKey) >= 0;
    }

    @Override
    public boolean containsColumn(Object columnKey) {
        return this.find(this.colKeys, this.colMask, columnKey) >= 0;
    }

    @Override
    public boolean containsValue(Object value) {
        return ArrayUtils.contains(this.values, value);
    }

    @Override
    public V get(Object rowKey, Object columnKey) {
        final int row = this.getIndex(this.rowKeys, this.rowIndices, this.rowMask, rowKey);
        final int col = this.getIndex(this.colKeys, this.colIndices, this.colMask, columnKey);

        if (row < 0 || col < 0) {
            return null;
        }

        return this.values[this.colCount * row + col];
    }

    @Override
    public boolean isEmpty() {
        return this.size() == 0;
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public V put(R rowKey, C columnKey, V val) {
        throw new UnsupportedOperationException();
    }

    private <T> int find(T[] key, int mask, T value) {
        T curr;
        int pos;
        // The starting point.
        if ((curr = key[pos = HashCommon.mix(value.hashCode()) & mask]) == null) {
            return -(pos + 1);
        }
        if (value.equals(curr)) {
            return pos;
        }
        // There's always an unused entry.
        while (true) {
            if ((curr = key[pos = pos + 1 & mask]) == null) {
                return -(pos + 1);
            }
            if (value.equals(curr)) {
                return pos;
            }
        }
    }

    @Override
    public void putAll(@NotNull Table<? extends R, ? extends C, ? extends V> table) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(Object rowKey, Object columnKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull Map<C, V> row(R rowKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull Map<R, V> column(C columnKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull Set<Cell<R, C, V>> cellSet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull Set<R> rowKeySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull Set<C> columnKeySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull Map<R, Map<C, V>> rowMap() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull Map<C, Map<R, V>> columnMap() {
        throw new UnsupportedOperationException();
    }
}
