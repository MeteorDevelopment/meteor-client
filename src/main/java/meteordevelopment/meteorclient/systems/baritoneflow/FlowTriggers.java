/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.baritoneflow;

import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Watches every armed {@link Flow}'s trigger nodes in the background and fires a callback the moment
 * a trigger's condition newly becomes true, so a flow can start (or interrupt a running flow) on its
 * own. Condition-based triggers (hunger, hostile mobs, low health, inventory full) are edge-detected:
 * they fire once on the rising edge and won't re-fire until the condition clears and returns.
 */
public class FlowTriggers {
    private final Map<FlowNode, Set<UUID>> nearbyPlayers = new HashMap<>();
    private final Map<FlowNode, Boolean> lastState = new HashMap<>();
    private final BiConsumer<Flow, FlowNode> onTrigger;

    public FlowTriggers(BiConsumer<Flow, FlowNode> onTrigger) {
        this.onTrigger = onTrigger;
    }

    public void tick() {
        if (mc.level == null || mc.player == null) return;

        for (Flow flow : BaritoneFlows.get()) {
            if (!flow.armed) continue;

            for (FlowNode node : flow.nodes) {
                if (!node.type.get().isTrigger()) continue;

                switch (node.type.get()) {
                    case OnPlayerNearAppear -> checkPlayerNearAppear(flow, node);
                    case OnHunger -> risingEdge(flow, node, mc.player.getFoodData().getFoodLevel() <= node.hungerThreshold.get());
                    case OnHostileMobsNear -> risingEdge(flow, node, countHostiles(node.triggerRange.get()) >= node.mobCount.get());
                    case OnLowHealth -> risingEdge(flow, node, mc.player.getHealth() <= node.healthThreshold.get());
                    case OnInventoryFull -> risingEdge(flow, node, freeSlots() <= node.freeSlots.get());
                    default -> {
                    }
                }
            }
        }
    }

    /**
     * Called immediately when the client disconnects, with the human-readable reason text (e.g. a
     * kick message). Fires every armed flow's {@link FlowNodeType#OnDisconnect} nodes whose filter
     * is empty or matches, unlike the tick-polled triggers above since a disconnect is a one-off event.
     */
    public void onDisconnect(String reason) {
        String lower = reason == null ? "" : reason.toLowerCase();

        for (Flow flow : BaritoneFlows.get()) {
            if (!flow.armed) continue;

            for (FlowNode node : flow.getNodesOfType(FlowNodeType.OnDisconnect)) {
                String filter = node.disconnectReasonFilter.get();
                if (filter == null || filter.isBlank() || lower.contains(filter.toLowerCase())) {
                    onTrigger.accept(flow, node);
                }
            }
        }
    }

    /** Fires once when {@code condition} transitions from false to true. */
    private void risingEdge(Flow flow, FlowNode node, boolean condition) {
        boolean was = Boolean.TRUE.equals(lastState.get(node));
        lastState.put(node, condition);

        if (condition && !was) onTrigger.accept(flow, node);
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

    private int countHostiles(double range) {
        int count = 0;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || !entity.isAlive()) continue;
            if (entity.getType().getCategory() != MobCategory.MONSTER) continue;
            if (PlayerUtils.isWithin(entity, range)) count++;
        }

        return count;
    }

    private int freeSlots() {
        int free = 0;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) free++;
        }

        return free;
    }

    public void clear() {
        nearbyPlayers.clear();
        lastState.clear();
    }
}
