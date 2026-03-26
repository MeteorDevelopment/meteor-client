/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.friends;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.util.UndashedUuid;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Friends extends System<Friends> implements Iterable<Friend> {
    private final List<Friend> friends = new ArrayList<>();

    public Friends() {
        super("friends");
    }

    public static Friends get() {
        return Systems.get(Friends.class);
    }

    public boolean add(Friend friend) {
        if (friend.name.isEmpty() || friend.name.contains(" ")) return false;

        if (!friends.contains(friend)) {
            friends.add(friend);
            save();
            return true;
        }

        return false;
    }

    public boolean remove(Friend friend) {
        if (friends.remove(friend)) {
            save();
            return true;
        }

        return false;
    }

    public Friend get(String name) {
        for (Friend friend : friends) {
            if (friend.name.equalsIgnoreCase(name)) return friend;
        }
        return null;
    }

    public Friend get(PlayerEntity player) {
        return get(player.getName().getString());
    }

    public Friend get(PlayerListEntry player) {
        return get(player.getProfile().name());
    }

    public boolean isFriend(PlayerEntity player) {
        return player != null && get(player) != null;
    }

    public boolean isFriend(PlayerListEntry player) {
        return get(player) != null;
    }

    public boolean shouldAttack(PlayerEntity player) {
        return !isFriend(player);
    }

    public int count() {
        return friends.size();
    }

    public boolean isEmpty() {
        return friends.isEmpty();
    }

    @Override
    public @NotNull Iterator<Friend> iterator() {
        return friends.iterator();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.put("friends", NbtUtils.listToTag(friends));
        return tag;
    }

    @Override
    public Friends fromTag(NbtCompound tag) {
        friends.clear();

        for (NbtElement itemTag : tag.getListOrEmpty("friends")) {
            NbtCompound friendTag = (NbtCompound) itemTag;
            if (!friendTag.contains("name")) continue;

            String name = friendTag.getString("name", "");
            if (get(name) != null) continue;

            String uuid = friendTag.getString("id", "");
            Friend friend = !uuid.isBlank()
                ? new Friend(name, UndashedUuid.fromStringLenient(uuid))
                : new Friend(name);

            friends.add(friend);
        }

        Collections.sort(friends);

        MeteorExecutor.execute(() -> friends.forEach(Friend::updateInfo));

        return this;
    }

    public int importFromMio() throws Exception {
        Path path = Paths.get(
            MinecraftClient.getInstance().runDirectory.getAbsolutePath(),
            "mio-fabric",
            "socials.json"
        );

        if (!Files.exists(path)) return -1;

        String json = Files.readString(path);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray socials = root.getAsJsonArray("socials");

        int added = 0;

        for (JsonElement element : socials) {
            JsonObject obj = element.getAsJsonObject();

            String role = obj.get("role").getAsString();
            if (!role.equalsIgnoreCase("friend")) continue;

            String name = obj.get("name").getAsString();

            if (get(name) == null) {
                add(new Friend(name));
                added++;
            }
        }

        return added;
    }
}
