/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.friends;

import com.mojang.util.UUIDTypeAdapter;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.render.color.RainbowColors;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class Friends extends System<Friends> implements Iterable<Friend> {
    private List<Friend> friends = new ArrayList<>();

    public final SettingColor color = new SettingColor(0, 255, 180);
    public boolean attack = false;

    public Friends() {
        super("friends");
    }

    public static Friends get() {
        return Systems.get(Friends.class);
    }

    @Override
    public void init() {
        RainbowColors.add(color);
    }

    public boolean add(Friend friend) {
        if (friend.name.isEmpty()) return false;

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
            if (friend.name.equals(name)) {
                return friend;
            }
        }

        return null;
    }

    public Friend get(UUID uuid) {
        for (Friend friend : friends) {
            if (friend.id.equals(uuid)) {
                return friend;
            }
        }

        return null;
    }

    public Friend get(PlayerEntity player) {
        return get(player.getUuid());
    }

    public boolean isFriend(PlayerEntity player) {
        return get(player) != null;
    }

    public boolean shouldAttack(PlayerEntity player) {
        return !isFriend(player) || attack;
    }

    public int count() {
        return friends.size();
    }

    @Override
    public @NotNull Iterator<Friend> iterator() {
        return friends.iterator();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        NbtList friendsTag = new NbtList();

        for (Friend friend : friends) friendsTag.add(friend.toTag());
        tag.put("friends", friendsTag);
        tag.put("color", color.toTag());
        tag.putBoolean("attack", attack);

        return tag;
    }

    @Override
    public Friends fromTag(NbtCompound tag) {
        NbtList friendList = tag.getList("friends", 10);

        friends = new ArrayList<>();
        for (NbtElement friendTag : friendList) {
            NbtCompound compound = (NbtCompound) friendTag;

            Friend friend;
            if (compound.contains("name")) {
                friend = getFromName(compound.getString("name"));
            } else {
                friend = new Friend(compound);
            }

            if (friend != null) friends.add(friend);
        }

        if (tag.contains("color")) color.fromTag(tag.getCompound("color"));
        attack = tag.contains("attack") && tag.getBoolean("attack");
        return this;
    }

    public Friend getFromName(String username) {
        Response resp = Http.get("https://api.mojang.com/users/profiles/minecraft/" + username).sendJson(Response.class);
        if (resp != null) {
            return new Friend(username, UUIDTypeAdapter.fromString(resp.id));
        }
        return null;
    }

    private static class Response {
        String id;
    }
}
