/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.groups;

import meteordevelopment.meteorclient.MeteorClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class ListGroup<T, G extends ListGroup<T, G>> {
    protected List<T> direct = new ArrayList<>();
    protected List<G> include = new ArrayList<>();

    final protected ListGroupTracker tracker;

    public boolean trackerIs(ListGroupTracker o) { return tracker == o; };

    @Unmodifiable
    public List<T> get() {
        return direct;
    }

    @Unmodifiable
    final public List<T> getAll() {
        return direct;
    }

    @Unmodifiable
    public List<G> getGroups() {
        return include;
    }

    public boolean add(T t) {
        if (!direct.contains(t)) {
            MeteorClient.LOG.info("ListGroup.add@ had {}", direct.size());
            direct.add(t);
            tracker.invalidate();
            return true;
        }
        MeteorClient.LOG.info("ListGroup.add@ duplicate item");
        return false;
    }

    public boolean add(G g) {
        if (!include.contains(g)) {
            include.add(g);
            tracker.invalidate();
            return true;
        }
        return false;
    }

    public boolean remove(G g) {
        if (include.remove(g)) {
            tracker.invalidate();
            return true;
        }
        return false;
    }

    public boolean remove(T t) {
        if (direct.remove(t)) {
            tracker.invalidate();
            return true;
        }
        return false;
    }

    public boolean addAll(@NotNull Collection<? extends T> collection) {
        boolean modified = false;
        for (T t : collection) {
            MeteorClient.LOG.info("ListGroup.addAll@ had {}", direct.size());
            if (!direct.contains(t)) {
                direct.add(t);
                modified = true;
            }
            MeteorClient.LOG.info("ListGroup.addAll@ has {}", direct.size());
        }
        if (modified) tracker.invalidate();
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
        if (modified) tracker.invalidate();;
        return modified;
    }

    public boolean removeAll(@NotNull Collection<T> collection) {
        tracker.invalidate();
        return direct.removeAll(collection);
    }

    public boolean removeAllGroups(@NotNull Collection<G> collection) {
        tracker.invalidate();
        return include.removeAll(collection);
    }

    public ListGroup(ListGroupTracker t) { tracker = t; }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }
}
