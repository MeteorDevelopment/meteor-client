package me.jellysquid.mods.lithium.common.util.collections;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Wraps a {@link List} with a hash table which provides O(1) lookups for {@link Collection#contains(Object)}. The type
 * contained by this list must use reference-equality semantics.
 */
@SuppressWarnings("SuspiciousMethodCalls")
public class HashedReferenceList<T> implements List<T> {
    private final ReferenceArrayList<T> list;
    private final Reference2IntOpenHashMap<T> counter;

    public HashedReferenceList(List<T> list) {
        this.list = new ReferenceArrayList<>();
        this.list.addAll(list);

        this.counter = new Reference2IntOpenHashMap<>();
        this.counter.defaultReturnValue(0);

        for (T obj : this.list) {
            this.counter.addTo(obj, 1);
        }
    }

    @Override
    public int size() {
        return this.list.size();
    }

    @Override
    public boolean isEmpty() {
        return this.list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.counter.containsKey(o);
    }

    @Override
    public Iterator<T> iterator() {
        return this.listIterator();
    }

    @Override
    public Object[] toArray() {
        return this.list.toArray();
    }

    @SuppressWarnings("SuspiciousToArrayCall")
    @Override
    public <T1> T1[] toArray(T1 @NotNull [] a) {
        return this.list.toArray(a);
    }

    @Override
    public boolean add(T t) {
        this.trackReferenceAdded(t);

        return this.list.add(t);
    }

    @Override
    public boolean remove(Object o) {
        this.trackReferenceRemoved(o);

        return this.list.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object obj : c) {
            if (!this.counter.containsKey(obj)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        for (T obj : c) {
            this.trackReferenceAdded(obj);
        }

        return this.list.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        for (T obj : c) {
            this.trackReferenceAdded(obj);
        }

        return this.list.addAll(index, c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        if (this.size() >= 2 && c.size() > 4 && c instanceof List) {
            //HashReferenceList uses reference equality, so using ReferenceOpenHashSet is fine
            c = new ReferenceOpenHashSet<>(c);
        }
        this.counter.keySet().removeAll(c);
        return this.list.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        this.counter.keySet().retainAll(c);
        return this.list.retainAll(c);
    }

    @Override
    public void clear() {
        this.counter.clear();
        this.list.clear();
    }

    @Override
    public T get(int index) {
        return this.list.get(index);
    }

    @Override
    public T set(int index, T element) {
        T prev = this.list.set(index, element);

        if (prev != element) {
            if (prev != null) {
                this.trackReferenceRemoved(prev);
            }

            this.trackReferenceAdded(element);
        }

        return prev;
    }

    @Override
    public void add(int index, T element) {
        this.trackReferenceAdded(element);

        this.list.add(index, element);
    }

    @Override
    public T remove(int index) {
        T prev = this.list.remove(index);

        if (prev != null) {
            this.trackReferenceRemoved(prev);
        }

        return prev;
    }

    @Override
    public int indexOf(Object o) {
        return this.list.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return this.list.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return this.listIterator(0);
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return new ListIterator<>() {
            private final ListIterator<T> inner = HashedReferenceList.this.list.listIterator(index);

            @Override
            public boolean hasNext() {
                return this.inner.hasNext();
            }

            @Override
            public T next() {
                return this.inner.next();
            }

            @Override
            public boolean hasPrevious() {
                return this.inner.hasPrevious();
            }

            @Override
            public T previous() {
                return this.inner.previous();
            }

            @Override
            public int nextIndex() {
                return this.inner.nextIndex();
            }

            @Override
            public int previousIndex() {
                return this.inner.previousIndex();
            }

            @Override
            public void remove() {
                int last = this.previousIndex();

                if (last == -1) {
                    throw new NoSuchElementException();
                }

                T prev = HashedReferenceList.this.get(last);

                if (prev != null) {
                    HashedReferenceList.this.trackReferenceRemoved(prev);
                }

                this.inner.remove();
            }

            @Override
            public void set(T t) {
                int last = this.previousIndex();

                if (last == -1) {
                    throw new NoSuchElementException();
                }

                T prev = HashedReferenceList.this.get(last);

                if (prev != t) {
                    if (prev != null) {
                        HashedReferenceList.this.trackReferenceRemoved(prev);
                    }

                    HashedReferenceList.this.trackReferenceAdded(t);
                }

                this.inner.remove();
            }

            @Override
            public void add(T t) {
                HashedReferenceList.this.trackReferenceAdded(t);

                this.inner.add(t);
            }
        };
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return this.list.subList(fromIndex, toIndex);
    }

    private void trackReferenceAdded(T t) {
        this.counter.addTo(t, 1);
    }

    @SuppressWarnings("unchecked")
    private void trackReferenceRemoved(Object o) {
        if (this.counter.addTo((T) o, -1) <= 1) {
            this.counter.removeInt(o);
        }
    }

}
