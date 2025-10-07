/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.groups;

import meteordevelopment.meteorclient.MeteorClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.Predicate;

public class GroupedList<T, G extends ListGroup<T, G>> implements Iterable<T> {
    private List<T> cached;
    private List<G> include;
    private List<T> direct;

    private Predicate<T> filter;

    public ListGroupTracker tracker = null;
    long version;

    private boolean isValid() {
        if (cached == null) return false;
        if (tracker == null) return false;
        return version == tracker.getVersion();
    }

    public GroupedList() {
        cached = null;
        include = new ArrayList<>();
        direct = new ArrayList<>();
    }

    public GroupedList(@NotNull Collection<T> d) {
        cached = new ArrayList<>(d);
        include = new ArrayList<>();
        direct = new ArrayList<>(d);
    }

    public GroupedList(Collection<T> d, @NotNull Collection<G> g) {
        cached = null;
        include = new ArrayList<>(g);
        direct = d == null ? new ArrayList<>() : new ArrayList<>(d);
    }

    public boolean add(T t) {
        if (!direct.contains(t)) {
            direct.add(t);
            if (isValid() && !cached.contains(t)) cached.add(t);
            return true;
        }
        return false;
    }

    public boolean add(G g) {
        if (!include.contains(g)) {
            include.add(g);
            cached = null;
            return true;
        }
        return false;
    }

    public boolean remove(G g) {
        if (include.remove(g)) {
            cached = null;
            return true;
        }
        return false;
    }

    public boolean remove(T t) {
        if (direct.remove(t)) {
            cached = null;
            return true;
        }
        return false;
    }

    public void invalidate() {
        cached = null;
    }

    public void setFilter(Predicate<T> filter) {
        this.filter = filter;
        cached = null;
    }

    public boolean testFilter(T t) {
        return filter.test(t);
    }

    @Unmodifiable
    public List<T> get() {

        if (isValid()) return cached;

        if (tracker != null) version = tracker.getVersion();
        else if (cached != null && !cached.isEmpty()) MeteorClient.LOG.warn("Rebuild of GroupedList with tracker == null");

        MeteorClient.LOG.info("Rebuild {} direct, {} groups.", direct.size(), include.size());

        Set<T> set = new HashSet<>(direct);
        List<ListGroup<T, G>> seen = new ArrayList<>();
        List<ListGroup<T, G>> next = new ArrayList<>();

        for (ListGroup<T, G> g : include) {
            g.internalGetAll(set, seen, next);
        }


        if (cached != null) cached.clear();
        else cached = new ArrayList<>();

        for (T t : set) {
            if (filter == null || filter.test(t)) cached.add(t);
        }

        return cached;
    }

    public void set(GroupedList<T, G> other) {
        cached = null;
        if (other.isValid()) {
            cached = new ArrayList<>(other.cached);
            version = tracker.getVersion();
        }
        include = new ArrayList<>(other.include);
        direct = new ArrayList<>(other.direct);
    }

    public void clear() {
        if (cached != null) cached.clear();
        include.clear();
        direct.clear();
    }

    @Unmodifiable
    public List<T> getDirectlyIncludedItems() {
        return direct;
    }

    @Unmodifiable
    public List<G> getIncludedGroups() {
        return include;
    }

    public boolean containsAll(@NotNull Collection<T> collection) {
        get();
        for (T t : collection) if (!cached.contains(t)) return false;
        return true;
    }

    public boolean addAll(@NotNull Collection<? extends T> collection) {
        boolean modified = false;
        for (T t : collection) {
            if (!direct.contains(t)) {
                direct.add(t);
                modified = true;
            }
        }
        if (modified) cached = null;
        return modified;
    }

    public boolean addAllGroups(Collection<G> collection) {
        boolean modified = false;
        for (G g : collection) {
            if (!include.contains(g)) {
                include.add(g);
                modified = true;
            }
        }
        if (modified) cached = null;
        return modified;
    }

    public boolean removeAll(@NotNull Collection<T> collection) {
        cached = null;
        return direct.removeAll(collection);
    }

    public boolean removeAllGroups(@NotNull Collection<G> collection) {
        cached = null;
        return include.removeAll(collection);
    }

    public int size() {
        return get().size();
    }

    public boolean isEmpty() {
        return get().isEmpty();
    }

    public boolean contains(Object o) {
        return get().contains(o);
    }

    @Override
    public @NotNull Iterator<T> iterator() {
        return get().iterator();
    }
}
