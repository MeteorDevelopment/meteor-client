/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.groups;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import meteordevelopment.meteorclient.MeteorClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.Predicate;

@Unmodifiable
public class GroupSet<T, G extends SetGroup<T, G>> implements Iterable<T> {
    private Set<T> cached;
    private Set<T> immediate;
    private List<G> include;

    private Predicate<T> includeIf;

    public SetGroupEnumeration enumeration = null;
    long version;

    private boolean isValid() {
        if (cached == null) return false;
        if (enumeration == null) return false;
        return version == enumeration.getVersion();
    }

    public GroupSet() {
        cached = null;
        include = new ArrayList<>();
        immediate = new ReferenceOpenHashSet<>();
    }

    public GroupSet(@NotNull Collection<T> d) {
        cached = new ReferenceOpenHashSet<>(d);
        include = new ArrayList<>();
        immediate = new ReferenceOpenHashSet<>(cached);
    }

    public GroupSet(Collection<T> d, @NotNull Collection<G> g) {
        cached = null;
        include = new ArrayList<>(g);
        immediate = d == null ? new ReferenceOpenHashSet<>() : new ReferenceOpenHashSet<>(d);
    }

    public boolean add(T t) {
        if (!immediate.contains(t)) {
            immediate.add(t);
            if (isValid()) cached.add(t);
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

    public boolean remove(T t) {
        if (immediate.remove(t)) {
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

    public void invalidate() {
        cached = null;
    }

    public void setIncludeCondition(Predicate<T> includeIf) {
        this.includeIf = includeIf;
        cached = null;
    }

    @Unmodifiable
    public Set<T> get() {

        if (isValid()) return cached;

        if (enumeration != null) version = enumeration.getVersion();
        // debug statement
        else if (cached != null && !cached.isEmpty()) MeteorClient.LOG.warn("Rebuild of temporary GroupSet");

        Set<T> set = new ReferenceOpenHashSet<>(immediate);
        List<SetGroup<T, G>> seen = new ArrayList<>();
        List<SetGroup<T, G>> next = new ArrayList<>();

        for (SetGroup<T, G> g : include) {
            g.internalGetAll(set, seen, next);
        }

        if (includeIf != null) set.removeIf((t) -> !includeIf.test(t));

        cached = set;

        return Collections.unmodifiableSet(cached);
    }

    public void set(GroupSet<T, G> other) {
        cached = null;
        if (other.isValid()) {
            cached = new ReferenceOpenHashSet<>(other.cached);
            version = enumeration.getVersion();
        }
        include = new ArrayList<>(other.include);
        immediate = new ReferenceOpenHashSet<>(other.immediate);
    }

    public void clear() {
        if (cached != null) cached.clear();
        include.clear();
        immediate.clear();
    }

    @Unmodifiable
    public Set<T> getImmediate() {
        return Collections.unmodifiableSet(immediate);
    }

    @Unmodifiable
    public List<G> getGroups() {
        return Collections.unmodifiableList(include);
    }

    public boolean containsAll(@NotNull Collection<T> collection) {
        get();
        for (T t : collection) if (!cached.contains(t)) return false;
        return true;
    }

    public boolean addAll(@NotNull Collection<T> collection) {
        boolean modified = false;
        for (T t : collection) {
            if (!immediate.contains(t)) {
                immediate.add(t);
                modified = true;
            }
        }
        if (modified) cached = null;
        return modified;
    }

    public boolean addAllGroups(@NotNull Collection<G> collection) {
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
        return immediate.removeAll(collection);
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

    public @NotNull Object[] toArray() {
        return get().toArray();
    }

    public @NotNull <T1> T1[] toArray(@NotNull T1[] t1s) {
        return get().toArray(t1s);
    }
}
