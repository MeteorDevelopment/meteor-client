/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.groups;


import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;


public interface IGroup<T, G extends SetGroup<T, G>> {
    @Unmodifiable
    Set<T> getImmediate();

    @Unmodifiable
    Set<T> getAll();
    @Unmodifiable
    Set<T> getAllMatching(Predicate<T> predicate);

    boolean anyMatch(Predicate<T> predicate);

    @Unmodifiable
    List<G> getGroups();

    boolean add(T t);
    boolean add(G g);

    boolean remove(G g);
    boolean remove(T t);

    boolean addAll(@NotNull Collection<? extends T> collection);

    boolean addAllGroups(Collection<G> collection);
    boolean removeAll(@NotNull Collection<T> collection);
    boolean removeAllGroups(@NotNull Collection<G> collection);

    default boolean containsAll(@NotNull Collection<T> collection) {
        return getAll().containsAll(collection);
    }

    default boolean isEmpty() {
        return getAll().isEmpty();
    }

    default boolean contains(Object o) {
        return getAll().contains(o);
    }

}
