/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.baritoneflow;

import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Persists all saved {@link Flow}s to disk so they can be reopened and run across sessions.
 */
public class BaritoneFlows extends System<BaritoneFlows> implements Iterable<Flow> {
    private List<Flow> flows = new ArrayList<>();

    public BaritoneFlows() {
        super("baritone-flows");
    }

    public static BaritoneFlows get() {
        return Systems.get(BaritoneFlows.class);
    }

    public void add(Flow flow) {
        flows.add(flow);
        save();
    }

    public void remove(Flow flow) {
        if (flows.remove(flow)) save();
    }

    public Flow get(String name) {
        for (Flow flow : flows) {
            if (flow.name.equalsIgnoreCase(name)) return flow;
        }
        return null;
    }

    public List<Flow> getAll() {
        return flows;
    }

    public boolean isEmpty() {
        return flows.isEmpty();
    }

    @Override
    public @NotNull Iterator<Flow> iterator() {
        return flows.iterator();
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("flows", NbtUtils.listToTag(flows));
        return tag;
    }

    @Override
    public BaritoneFlows fromTag(CompoundTag tag) {
        flows = NbtUtils.listFromTag(tag.getListOrEmpty("flows"), Flow::new);
        return this;
    }
}
