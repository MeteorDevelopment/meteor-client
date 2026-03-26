/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.friends;

import com.mojang.util.UndashedUuid;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

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
            if (friend.name.equalsIgnoreCase(name)) {
                return friend;
            }
        }

        return null;
    }

    public Friend get(Player player) {
        return get(player.getName().getString());
    }

    public Friend get(PlayerInfo player) {
        return get(player.getProfile().name());
    }

    public boolean isFriend(Player player) {
        return player != null && get(player) != null;
    }

    public boolean isFriend(PlayerInfo player) {
        return get(player) != null;
    }

    public boolean shouldAttack(Player player) {
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
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.put("friends", NbtUtils.listToTag(friends));

        return tag;
    }

    @Override
    public Friends fromTag(CompoundTag tag) {
        friends.clear();

        for (Tag itemTag : tag.getListOrEmpty("friends")) {
            CompoundTag friendTag = (CompoundTag) itemTag;
            if (!friendTag.contains("name")) continue;

            String name = friendTag.getStringOr("name", "");
            if (get(name) != null) continue;

            String uuid = friendTag.getStringOr("id", "");
            Friend friend = !uuid.isBlank()
                ? new Friend(name, UndashedUuid.fromStringLenient(uuid))
                : new Friend(name);

            friends.add(friend);
        }

        Collections.sort(friends);

        MeteorExecutor.execute(() -> friends.forEach(Friend::updateInfo));

        return this;
    }
}
