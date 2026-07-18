/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.baritoneflow;

import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Watches every armed {@link Flow}'s trigger nodes (e.g. {@link FlowNodeType#OnPlayerNearAppear})
 * in the background and fires a callback the moment a trigger's condition newly becomes true, so a
 * flow can start on its own instead of only through a manual Run.
 */
public class FlowTriggers {
    private final Map<FlowNode, Set<UUID>> nearbyPlayers = new HashMap<>();
    private final BiConsumer<Flow, FlowNode> onTrigger;

    public FlowTriggers(BiConsumer<Flow, FlowNode> onTrigger) {
        this.onTrigger = onTrigger;
    }

    public void tick() {
        if (mc.level == null || mc.player == null) return;

        for (Flow flow : BaritoneFlows.get()) {
            if (!flow.armed) continue;

            for (FlowNode node : flow.getNodesOfType(FlowNodeType.OnPlayerNearAppear)) {
                checkPlayerNearAppear(flow, node);
            }
        }
    }

    private void checkPlayerNearAppear(Flow flow, FlowNode node) {
        Set<UUID> previous = nearbyPlayers.computeIfAbsent(node, _ -> new HashSet<>());
        Set<UUID> current = new HashSet<>();
        boolean newAppearance = false;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Player player) || player == mc.player) continue;
            if (!PlayerUtils.isWithin(entity, node.triggerRange.get())) continue;
            if (node.ignoreFriends.get() && Friends.get().isFriend(player)) continue;

            UUID id = player.getUUID();
            current.add(id);
            if (!previous.contains(id)) newAppearance = true;
        }

        nearbyPlayers.put(node, current);

        if (newAppearance) onTrigger.accept(flow, node);
    }

    public void clear() {
        nearbyPlayers.clear();
    }
}
