/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Ignores extends System<Ignores> implements Iterable<String> {
    private final List<String> list = new ArrayList<>();

    public Ignores() {
        super("ignores");
    }

    public static Ignores get() {
        return Systems.get(Ignores.class);
    }

    @Override
    public void init() {}

    public void add(String ignore) {
        list.add(ignore);
    }

    public boolean remove(String ignore) {
        return list.remove(ignore);
    }

    public int count() {
        return list.size();
    }

    @NotNull
    @Override
    public Iterator<String> iterator() {
        return list.iterator();
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        ListTag listTag = new ListTag();
        for (String ignore : list) listTag.add(StringTag.of(ignore));
        tag.put("list", listTag);

        return tag;
    }

    @Override
    public Ignores fromTag(CompoundTag tag) {
        list.clear();

        for (Tag t : tag.getList("list", 8)) {
            list.add(t.asString());
        }

        return this;
    }
}
