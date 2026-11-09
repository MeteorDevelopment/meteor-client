/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.playerhistory;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class PlayerHistory extends System<PlayerHistory> {
    private static final long RECORD_INTERVAL_MS = 30_000;
    private static final int MAX_ENTRIES = 10_000;
    private static final int MAX_RESULTS = 5;

    private final List<Sighting> sightings = new ArrayList<>();
    private final Map<UUID, Long> lastRecorded = new HashMap<>();
    private long nextPeriodicRecord;

    public PlayerHistory() {
        super(null);
    }

    public static PlayerHistory get() {
        return Systems.get(PlayerHistory.class);
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        sightings.clear();
        lastRecorded.clear();
        load();
        nextPeriodicRecord = 0;
    }

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (event.entity instanceof Player player) record(player);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (System.currentTimeMillis() < nextPeriodicRecord || MeteorClient.mc.level == null) return;

        nextPeriodicRecord = System.currentTimeMillis() + RECORD_INTERVAL_MS;
        for (Player player : MeteorClient.mc.level.players()) record(player);
    }

    private void record(Player player) {
        if (MeteorClient.mc.player == null || MeteorClient.mc.level == null || player == MeteorClient.mc.player) return;

        long now = System.currentTimeMillis();
        Long previous = lastRecorded.get(player.getUUID());
        if (previous != null && now - previous < RECORD_INTERVAL_MS) return;
        lastRecorded.put(player.getUUID(), now);

        sightings.add(new Sighting(
            player.getUUID(),
            player.getName().getString(),
            now,
            MeteorClient.mc.level.dimension().identifier().toString(),
            player.getBlockX(),
            player.getBlockY(),
            player.getBlockZ(),
            getGear(player)
        ));

        if (sightings.size() > MAX_ENTRIES) sightings.removeFirst();
        save();
    }

    private String getGear(Player player) {
        return "head=" + itemName(player.getItemBySlot(EquipmentSlot.HEAD))
            + ", chest=" + itemName(player.getItemBySlot(EquipmentSlot.CHEST))
            + ", legs=" + itemName(player.getItemBySlot(EquipmentSlot.LEGS))
            + ", feet=" + itemName(player.getItemBySlot(EquipmentSlot.FEET))
            + ", mainhand=" + itemName(player.getMainHandItem())
            + ", offhand=" + itemName(player.getOffhandItem());
    }

    private String itemName(ItemStack stack) {
        return stack.isEmpty() ? "empty" : stack.getHoverName().getString();
    }

    public List<Sighting> search(String name) {
        String query = name.toLowerCase(Locale.ROOT);
        return sightings.stream()
            .filter(sighting -> sighting.name.toLowerCase(Locale.ROOT).contains(query))
            .sorted(Comparator.comparingLong((Sighting sighting) -> sighting.timestamp).reversed())
            .limit(MAX_RESULTS)
            .toList();
    }

    public int size() {
        return sightings.size();
    }

    public void clear() {
        sightings.clear();
        lastRecorded.clear();
        save();
    }

    @Override
    public File getFile() {
        if (!Utils.canUpdate()) return null;
        return new File(new File(MeteorClient.FOLDER, "player-history"), Utils.getFileWorldName() + ".nbt");
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Sighting sighting : sightings) list.add(sighting.toTag());
        tag.put("sightings", list);
        return tag;
    }

    @Override
    public PlayerHistory fromTag(CompoundTag tag) {
        sightings.clear();
        lastRecorded.clear();

        for (Tag item : tag.getListOrEmpty("sightings")) {
            if (!(item instanceof CompoundTag sightingTag)) continue;
            try {
                Sighting sighting = new Sighting(sightingTag);
                sightings.add(sighting);
                lastRecorded.merge(sighting.uuid, sighting.timestamp, Math::max);
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed records while preserving the rest of the history.
            }
        }

        if (sightings.size() > MAX_ENTRIES) {
            sightings.subList(0, sightings.size() - MAX_ENTRIES).clear();
        }
        return this;
    }

    public static class Sighting {
        public final UUID uuid;
        public final String name;
        public final long timestamp;
        public final String dimension;
        public final int x, y, z;
        public final String gear;

        private Sighting(UUID uuid, String name, long timestamp, String dimension, int x, int y, int z, String gear) {
            this.uuid = uuid;
            this.name = name;
            this.timestamp = timestamp;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.gear = gear;
        }

        private Sighting(CompoundTag tag) {
            this.uuid = tag.read("uuid", UUIDUtil.CODEC).orElseThrow(() -> new IllegalArgumentException("Missing UUID"));
            this.name = tag.getStringOr("name", "");
            this.timestamp = tag.getLongOr("timestamp", 0);
            this.dimension = tag.getStringOr("dimension", "unknown");
            this.x = tag.getIntOr("x", 0);
            this.y = tag.getIntOr("y", 0);
            this.z = tag.getIntOr("z", 0);
            this.gear = tag.getStringOr("gear", "unknown");
        }

        private CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.store("uuid", UUIDUtil.CODEC, uuid);
            tag.putString("name", name);
            tag.putLong("timestamp", timestamp);
            tag.putString("dimension", dimension);
            tag.putInt("x", x);
            tag.putInt("y", y);
            tag.putInt("z", z);
            tag.putString("gear", gear);
            return tag;
        }
    }
}
