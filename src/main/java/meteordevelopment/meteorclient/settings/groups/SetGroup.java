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

public abstract class SetGroup<T, G extends SetGroup<T, G>> {
    protected Set<T> immediate = new ReferenceOpenHashSet<>();
    protected List<G> include = new ArrayList<>();

    final protected SetGroupEnumeration enumeration;

    public boolean isOf(SetGroupEnumeration of) { return enumeration == of; };

    @Unmodifiable
    public Set<T> getImmediate() {
        return immediate;
    }

    public Set<T> getAll() {
        Set<T> set = new ReferenceOpenHashSet<>();
        List<SetGroup<T, G>> seen = new ArrayList<>();
        List<SetGroup<T, G>> next = new ArrayList<>();
        internalGetAll(set, seen, next);
        return set;
    }

    public void internalGetAll(Collection<T> to, Collection<SetGroup<T, G>> seen, List<SetGroup<T, G>> next) {
        next.clear();
        next.add(this);
        for (int i = 0; i < next.size(); i++) {
            SetGroup<T, G> g = next.get(i);
            if (seen.contains(g)) continue;
            to.addAll(g.immediate);
            next.addAll(g.include);
            seen.add(g);
        }
    }

    public boolean anyMatch(Predicate<T> predicate) {
        List<SetGroup<T, G>> seen = new ArrayList<>();
        List<SetGroup<T, G>> next = new ArrayList<>();
        next.add(this);
        for (int i = 0; i < next.size(); i++) {
            SetGroup<T, G> g = next.get(i);
            if (seen.contains(g)) continue;
            if (g.immediate.stream().anyMatch(predicate)) return true;
            next.addAll(g.include);
            seen.add(g);
        }
        return false;
    }

    @Unmodifiable
    public List<G> getGroups() {
        return include;
    }

    public boolean add(T t) {
        if (!immediate.contains(t)) {
            MeteorClient.LOG.info("SetGroup.add@ had {}", immediate.size());
            immediate.add(t);
            enumeration.invalidate();
            return true;
        }
        MeteorClient.LOG.info("SetGroup.add@ duplicate item");
        return false;
    }

    public boolean add(G g) {
        if (!include.contains(g)) {
            include.add(g);
            enumeration.invalidate();
            return true;
        }
        return false;
    }

    public boolean remove(G g) {
        if (include.remove(g)) {
            enumeration.invalidate();
            return true;
        }
        return false;
    }

    public boolean remove(T t) {
        if (immediate.remove(t)) {
            enumeration.invalidate();
            return true;
        }
        return false;
    }

    public boolean addAll(@NotNull Collection<? extends T> collection) {
        boolean modified = false;
        for (T t : collection) {
            MeteorClient.LOG.info("SetGroup.addAll@ had {}", immediate.size());
            if (!immediate.contains(t)) {
                immediate.add(t);
                modified = true;
            }
            MeteorClient.LOG.info("SetGroup.addAll@ has {}", immediate.size());
        }
        if (modified) enumeration.invalidate();
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
        if (modified) enumeration.invalidate();;
        return modified;
    }

    public boolean removeAll(@NotNull Collection<T> collection) {
        enumeration.invalidate();
        return immediate.removeAll(collection);
    }

    public boolean removeAllGroups(@NotNull Collection<G> collection) {
        enumeration.invalidate();
        return include.removeAll(collection);
    }

    public SetGroup(SetGroupEnumeration t) { enumeration = t; }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }
}
