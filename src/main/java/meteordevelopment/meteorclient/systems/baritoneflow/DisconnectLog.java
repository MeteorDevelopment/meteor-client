/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.baritoneflow;

import meteordevelopment.meteorclient.events.game.DisconnectReasonEvent;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Keeps a small history of every disconnect (kicks, timeouts, our own Leave nodes, ...) with its
 * reason, so it can be reviewed later regardless of whether any module was active at the time.
 */
public class DisconnectLog extends System<DisconnectLog> {
    private static final int MAX_ENTRIES = 100;

    private final List<Entry> entries = new ArrayList<>();

    public DisconnectLog() {
        super("disconnect-log");
    }

    public static DisconnectLog get() {
        return Systems.get(DisconnectLog.class);
    }

    @EventHandler
    private void onDisconnectReason(DisconnectReasonEvent event) {
        entries.addFirst(new Entry(event.reason, java.lang.System.currentTimeMillis()));
        while (entries.size() > MAX_ENTRIES) entries.removeLast();

        save();
    }

    public List<Entry> getAll() {
        return Collections.unmodifiableList(entries);
    }

    public void clear() {
        entries.clear();
        save();
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        ListTag list = new ListTag();
        for (Entry entry : entries) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("reason", entry.reason());
            entryTag.putLong("time", entry.time());
            list.add(entryTag);
        }
        tag.put("entries", list);

        return tag;
    }

    @Override
    public DisconnectLog fromTag(CompoundTag tag) {
        entries.clear();

        for (Tag t : tag.getListOrEmpty("entries")) {
            CompoundTag entryTag = (CompoundTag) t;
            entries.add(new Entry(entryTag.getStringOr("reason", ""), entryTag.getLongOr("time", 0)));
        }

        return this;
    }

    public record Entry(String reason, long time) {}
}
