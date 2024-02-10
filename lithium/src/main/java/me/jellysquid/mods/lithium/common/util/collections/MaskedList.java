package me.jellysquid.mods.lithium.common.util.collections;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.*;
import java.util.function.Consumer;

public class MaskedList<E> extends AbstractList<E> {
    private final ObjectArrayList<E> allElements;
    private final BitSet visibleMask;
    private final Object2IntOpenHashMap<E> element2Index;
    private final boolean defaultVisibility;
    private int numCleared;

    public MaskedList(ObjectArrayList<E> allElements, boolean defaultVisibility) {
        this.allElements = new ObjectArrayList<>();
        this.visibleMask = new BitSet();
        this.defaultVisibility = defaultVisibility;
        this.element2Index = new Object2IntOpenHashMap<>();
        this.element2Index.defaultReturnValue(-1);

        this.addAll(allElements);
    }

    public MaskedList() {
        this(new ObjectArrayList<>(), true);
    }

    public int totalSize() {
        return this.allElements.size();
    }


    public void addOrSet(E element, boolean visible) {
        int index = this.element2Index.getInt(element);
        if (index != -1) {
            this.visibleMask.set(index, visible);
        } else {
            this.add(element);
            this.setVisible(element, visible);
        }
    }

    public void setVisible(E element, final boolean visible) {
        int index = this.element2Index.getInt(element);
        if (index != -1) {
            this.visibleMask.set(index, visible);
        }
        //ignore when the element is not in the collection
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<>() {
            int nextIndex = 0;
            int cachedNext = -1;

            @Override
            public boolean hasNext() {
                return (this.cachedNext = MaskedList.this.visibleMask.nextSetBit(this.nextIndex)) != -1;
            }

            @Override
            public E next() {
                int index = this.cachedNext;
                this.cachedNext = -1;
                this.nextIndex = index + 1;
                return MaskedList.this.allElements.get(index);
            }
        };
    }

    @Override
    public Spliterator<E> spliterator() {
        return new Spliterators.AbstractSpliterator<E>(Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.NONNULL) {
            int nextIndex = 0;

            @Override
            public boolean tryAdvance(Consumer<? super E> action) {
                int index = MaskedList.this.visibleMask.nextSetBit(this.nextIndex);
                if (index == -1) {
                    return false;
                }
                this.nextIndex = index + 1;
                action.accept(MaskedList.this.allElements.get(index));
                return true;
            }
        };
    }

    @Override
    public boolean add(E e) {
        int oldIndex = this.element2Index.put(e, this.allElements.size());
        if (oldIndex != -1) {
            throw new IllegalStateException("MaskedList must not contain duplicates! Trying to add " + e + " but it is already present at index " + oldIndex + ". Current size: " + this.allElements.size());
        }
        this.visibleMask.set(this.allElements.size(), this.defaultVisibility);
        return this.allElements.add(e);
    }

    @Override
    public boolean remove(Object o) {
        int index = this.element2Index.removeInt(o);
        if (index == -1) {
            return false;
        }
        this.visibleMask.clear(index);
        this.allElements.set(index, null);
        this.numCleared++;


        if (this.numCleared * 2 > this.allElements.size()) {
            ObjectArrayList<E> clonedElements = this.allElements.clone();
            BitSet clonedVisibleMask = (BitSet) this.visibleMask.clone();
            this.allElements.clear();
            this.visibleMask.clear();
            this.element2Index.clear();
            for (int i = 0; i < clonedElements.size(); i++) {
                E element = clonedElements.get(i);
                int newIndex = this.allElements.size();
                this.allElements.add(element);
                this.visibleMask.set(newIndex, clonedVisibleMask.get(i));
                this.element2Index.put(element, newIndex);
            }
            this.numCleared = 0;
        }
        return true;
    }

    @Override
    public E get(int index) {
        if (index < 0 || index >= this.size()) {
            throw new IndexOutOfBoundsException(index);
        }

        int i = 0;
        while (index >= 0) {
            index--;
            i = this.visibleMask.nextSetBit(i + 1);
        }
        return this.allElements.get(i);
    }

    @Override
    public int size() {
        return this.visibleMask.cardinality();
    }
}
