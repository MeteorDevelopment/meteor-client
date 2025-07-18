/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings.base;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.util.collection.IndexedIterable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class SortingHelper {
    private static final Comparator<Entry<?>> FILTER_COMPARATOR = Comparator.comparingInt(Entry::distance);

    private SortingHelper() {}

    public static <T> Iterable<T> sort(Iterable<T> registry, Predicate<T> filter, Function<T, String[]> nameFunction, String filterText) {
        return sortInternal(registry, filter, nameFunction, filterText, null);
    }

    public static <T> Iterable<T> sortWithPriority(Iterable<T> registry, Predicate<T> filter, Function<T, String[]> nameFunction, String filterText, Comparator<T> comparator) {
        return sortInternal(registry, filter, nameFunction, filterText, comparator);
    }

    private static <T> Iterable<T> sortInternal(Iterable<T> registry, Predicate<T> filter, Function<T, String[]> nameFunction, String filterText, @Nullable Comparator<T> comparator) {
        if (filterText.isBlank()) {
            if (comparator == null) {
                return filtering(registry, filter);
            } else {
                List<T> list = createList(registry);

                for (T value : registry) {
                    if (filter.test(value)) {
                        list.add(value);
                    }
                }

                list.sort(comparator);

                return list;
            }
        } else {
            List<Entry<T>> list = createList(registry);

            for (T value : registry) {
                if (!filter.test(value)) {
                    continue;
                }

                String[] names = nameFunction.apply(value);
                int bestWords = 0;
                int bestDistance = Integer.MAX_VALUE;
                float relevancy = 0f;
                for (String name : names) {
                    int words = Utils.searchInWords(name, filterText);
                    int distance = Utils.searchLevenshteinDefault(name, filterText, false);
                    bestWords = Math.max(bestWords, words);
                    bestDistance = Math.min(bestDistance, distance);
                    relevancy = Math.max(relevancy, 1f - (float) distance / name.length());
                }

                if (bestWords > 0 || relevancy >= 0.5f) {
                    list.add(new Entry<>(value, bestDistance));
                }
            }

            Comparator<Entry<T>> entryComparator = comparator != null
                ? Comparator.<Entry<T>, T>comparing(Entry::value, comparator).thenComparing(filterComparator())
                : filterComparator();

            list.sort(entryComparator);

            return iterate(list);
        }
    }

    private static <T> List<T> createList(Iterable<?> iterable) {
        if (iterable instanceof IndexedIterable<?> indexed) {
            return new ObjectArrayList<>(indexed.size());
        } else if (iterable instanceof Collection<?> collection) {
            return new ObjectArrayList<>(collection.size());
        } else {
            return new ObjectArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Comparator<Entry<T>> filterComparator() {
        return (Comparator<Entry<T>>) (Object) FILTER_COMPARATOR;
    }

    private static <T> Iterable<T> iterate(List<Entry<T>> sortedList) {
        return new Iterable<>() {
            @NotNull
            @Override
            public Iterator<T> iterator() {
                return new Iterator<>() {
                    private final Iterator<Entry<T>> it = sortedList.iterator();

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public T next() {
                        return it.next().value();
                    }
                };
            }
        };
    }

    private static <T> Iterable<T> filtering(Iterable<T> iterable, Predicate<T> filter) {
        return new Iterable<>() {
            @NotNull
            @Override
            public Iterator<T> iterator() {
                throw new UnsupportedOperationException("iterator() not supported by this Iterable, use forEach() instead.");
            }

            @Override
            public void forEach(Consumer<? super T> action) {
                for (T value : iterable) {
                    if (filter.test(value)) {
                        action.accept(value);
                    }
                }
            }
        };
    }

    public record Entry<T>(T value, int distance) {}
}
