/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.notebot.song;

import com.google.common.collect.Multimap;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Song {
    private final Multimap<Integer, Note> notesMap;
    private int lastTick;
    private final String title;
    private final String author;

    private final Set<Note> requirements = new HashSet<>();

    private boolean finishedLoading = false;

    public Song(Multimap<Integer, Note> notesMap,
                String title, String author) {
        this.notesMap = notesMap;
        this.title = title;
        this.author = author;
    }

    public void finishLoading() {
        if (finishedLoading) throw new IllegalStateException("Song has already finished loading!");

        this.lastTick = Collections.max(notesMap.keySet());
        this.notesMap.values().stream().distinct().forEach(requirements::add);

        finishedLoading = true;
    }

    public Multimap<Integer, Note> getNotesMap() {
        return notesMap;
    }

    public Set<Note> getRequirements() {
        if (!finishedLoading) throw new IllegalStateException("Song is still loading!");
        return requirements;
    }

    public int getLastTick() {
        if (!finishedLoading) throw new IllegalStateException("Song is still loading!");
        return lastTick;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }
}
