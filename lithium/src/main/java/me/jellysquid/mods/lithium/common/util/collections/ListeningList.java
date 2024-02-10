package me.jellysquid.mods.lithium.common.util.collections;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class ListeningList<T> implements List<T> {
    private final List<T> delegate;
    private final Runnable changeCallback;

    public ListeningList(List<T> delegate, Runnable changeCallback) {
        this.delegate = delegate;
        this.changeCallback = changeCallback;
    }

    protected void onChange() {
        this.changeCallback.run();
    }


    @Override
    public int size() {
        return this.delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.delegate.contains(o);
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return this.listIterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return this.delegate.toArray();
    }

    @Override
    public void forEach(Consumer<? super T> consumer) {
        this.delegate.forEach(consumer);
    }

    @NotNull
    @Override
    public <T1> T1[] toArray(@NotNull T1[] t1s) {
        //noinspection SuspiciousToArrayCall
        return this.delegate.toArray(t1s);
    }

    @Override
    public boolean add(T t) {
        boolean add = this.delegate.add(t);
        this.onChange();
        //noinspection ConstantConditions
        return add;
    }

    @Override
    public boolean remove(Object o) {
        boolean remove = this.delegate.remove(o);
        this.onChange();
        return remove;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> collection) {
        return this.delegate.containsAll(collection);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> collection) {
        boolean addAll = this.delegate.addAll(collection);
        this.onChange();
        return addAll;
    }

    @Override
    public boolean addAll(int i, @NotNull Collection<? extends T> collection) {
        boolean addAll = this.delegate.addAll(i, collection);
        this.onChange();
        return addAll;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> collection) {
        boolean b = this.delegate.removeAll(collection);
        this.onChange();
        return b;
    }

    @Override
    public boolean removeIf(Predicate<? super T> predicate) {
        boolean b = this.delegate.removeIf(predicate);
        this.onChange();
        return b;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> collection) {
        boolean b = this.delegate.retainAll(collection);
        this.onChange();
        return b;
    }

    @Override
    public void replaceAll(UnaryOperator<T> unaryOperator) {
        this.delegate.replaceAll( unaryOperator);
        this.onChange();
    }

    @Override
    public void sort(Comparator<? super T> comparator) {
        this.delegate.sort(comparator);
        this.onChange();
    }

    @Override
    public void clear() {
        this.delegate.clear();
        this.onChange();
    }

    @Override
    public T get(int i) {
        return this.delegate.get(i);
    }

    @Override
    public T set(int i, T t) {
        T set = this.delegate.set(i, t);
        this.onChange();
        return set;
    }

    @Override
    public void add(int i, T t) {
        this.delegate.add(i, t);
        this.onChange();
    }

    @Override
    public T remove(int i) {
        T remove = this.delegate.remove(i);
        this.onChange();
        return remove;
    }

    @Override
    public int indexOf(Object o) {
        return this.delegate.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return this.delegate.lastIndexOf(o);
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator() {
        return this.listIterator(0);
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator(int i) {
        return new ListIterator<T>() {
            final ListIterator<T> itDelegate = ListeningList.this.delegate.listIterator(i);

            @Override
            public boolean hasNext() {
                return this.itDelegate.hasNext();
            }

            @Override
            public T next() {
                return this.itDelegate.next();
            }

            @Override
            public boolean hasPrevious() {
                return this.itDelegate.hasPrevious();
            }

            @Override
            public T previous() {
                return this.itDelegate.previous();
            }

            @Override
            public int nextIndex() {
                return this.itDelegate.nextIndex();
            }

            @Override
            public int previousIndex() {
                return this.itDelegate.previousIndex();
            }

            @Override
            public void remove() {
                this.itDelegate.remove();
                ListeningList.this.onChange();
            }

            @Override
            public void set(T t) {
                this.itDelegate.set(t);
                ListeningList.this.onChange();

            }

            @Override
            public void add(T t) {
                this.itDelegate.add(t);
                ListeningList.this.onChange();
            }
        };
    }

    @NotNull
    @Override
    public List<T> subList(int i, int i1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Spliterator<T> spliterator() {
        return this.delegate.spliterator();
    }

    @Override
    public Stream<T> stream() {
        return this.delegate.stream();
    }

    @Override
    public Stream<T> parallelStream() {
        return this.delegate.parallelStream();
    }
}
