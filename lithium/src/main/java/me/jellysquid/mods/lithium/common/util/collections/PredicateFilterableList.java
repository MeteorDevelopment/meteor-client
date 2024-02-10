package me.jellysquid.mods.lithium.common.util.collections;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.function.Predicate;

public class PredicateFilterableList<T> extends ListeningList<T> {
    private Reference2ReferenceOpenHashMap<Predicate<T>, PredicateFilteredList> predicateToFiltered;

    public PredicateFilterableList() {
        super(new ArrayList<>(), null);
    }

    protected void onChange() {
        this.predicateToFiltered = null;
    }

    public PredicateFilteredList getFiltered(Predicate<T> predicate) {
        if (this.predicateToFiltered == null) {
            this.predicateToFiltered = new Reference2ReferenceOpenHashMap<>();
        }
        return this.predicateToFiltered.computeIfAbsent(predicate, PredicateFilteredList::new);
    }

    public class PredicateFilteredList extends AbstractList<T> {
        private final Predicate<T> predicate;
        private final ArrayList<T> delegate;
        private int filteredUpToIndex;

        private PredicateFilteredList(Predicate<T> predicate) {
            this.predicate = predicate;
            this.delegate = new ArrayList<>();
            this.filteredUpToIndex = 0;
        }

        @Override
        public T get(int index) {
            while (index >= this.delegate.size()) {
                if (this.filteredUpToIndex >= PredicateFilterableList.this.size()) {
                    return null;
                }
                T nextEntry = PredicateFilterableList.this.get(this.filteredUpToIndex);
                this.filteredUpToIndex++;
                if (this.predicate.test(nextEntry)) {
                    this.delegate.add(nextEntry);
                }
            }
            return this.delegate.get(index);
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }
    }
}
