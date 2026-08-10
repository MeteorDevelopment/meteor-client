/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.baritoneflow;

import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A named, saveable graph of {@link FlowNode}s. Nodes are connected via their child ids; execution
 * order is a depth-first pre-order traversal from the {@link FlowNodeType#Start} node.
 */
public class Flow implements ISerializable<Flow> {
    public String name;
    /** While armed, this flow's trigger nodes (e.g. {@link FlowNodeType#OnPlayerNearAppear}) are watched in the background and can start it on their own. */
    public boolean armed;
    public final List<FlowNode> nodes = new ArrayList<>();
    private int nextId = 1;

    public Flow(String name) {
        this.name = name;

        // Every flow starts with a Start node as its entry point.
        FlowNode start = addNode(40, 60);
        start.type.set(FlowNodeType.Start);
    }

    public Flow(Tag tag) {
        fromTag((CompoundTag) tag);
    }

    public FlowNode addNode(int x, int y) {
        FlowNode node = new FlowNode(nextId++, x, y);
        nodes.add(node);
        return node;
    }

    public void removeNode(FlowNode node) {
        if (node.type.get() == FlowNodeType.Start) return; // never delete the entry point

        nodes.remove(node);
        for (FlowNode n : nodes) n.children.removeIf(id -> id == node.id);
    }

    public FlowNode getById(int id) {
        for (FlowNode n : nodes) {
            if (n.id == id) return n;
        }
        return null;
    }

    public FlowNode getStart() {
        for (FlowNode n : nodes) {
            if (n.type.get() == FlowNodeType.Start) return n;
        }
        return nodes.isEmpty() ? null : nodes.getFirst();
    }

    public List<FlowNode> getNodesOfType(FlowNodeType type) {
        List<FlowNode> result = new ArrayList<>();
        for (FlowNode n : nodes) {
            if (n.type.get() == type) result.add(n);
        }
        return result;
    }

    public void connect(FlowNode from, FlowNode to) {
        if (from == to) return;
        if (!from.children.contains(to.id)) from.children.add(to.id);
    }

    public void disconnect(FlowNode from, FlowNode to) {
        from.children.removeIf(id -> id == to.id);
    }

    /** Depth-first pre-order traversal from the Start node. See {@link #computeOrder(FlowNode)}. */
    public List<FlowNode> computeOrder() {
        FlowNode start = getStart();
        return start == null ? new ArrayList<>() : computeOrder(start);
    }

    /**
     * Depth-first pre-order traversal starting from the given node (used both for a manual run from
     * Start and for a trigger node firing on its own). Already visited nodes are skipped, so loops
     * in the graph terminate instead of running forever.
     */
    public List<FlowNode> computeOrder(FlowNode entry) {
        List<FlowNode> order = new ArrayList<>();
        if (entry == null) return order;

        Set<Integer> visited = new HashSet<>();
        Deque<FlowNode> stack = new ArrayDeque<>();
        stack.push(entry);

        while (!stack.isEmpty()) {
            FlowNode node = stack.pop();
            if (!visited.add(node.id)) continue;
            order.add(node);

            // Push children in reverse so the first child is processed first.
            for (int i = node.children.size() - 1; i >= 0; i--) {
                FlowNode child = getById(node.children.get(i));
                if (child != null && !visited.contains(child.id)) stack.push(child);
            }
        }

        return order;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putString("name", name);
        tag.putBoolean("armed", armed);
        tag.putInt("nextId", nextId);
        tag.put("nodes", NbtUtils.listToTag(nodes));

        return tag;
    }

    @Override
    public Flow fromTag(CompoundTag tag) {
        name = tag.getStringOr("name", "Flow");
        armed = tag.getBooleanOr("armed", false);
        nextId = tag.getIntOr("nextId", 1);

        nodes.clear();
        nodes.addAll(NbtUtils.listFromTag(tag.getListOrEmpty("nodes"), FlowNode::new));

        return this;
    }
}
