/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.friends;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.NbtUtils;
import minegame159.meteorclient.utils.Savable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FriendManager extends Savable<FriendManager> implements Iterable<Friend> {
    public static final FriendManager INSTANCE = new FriendManager();

    private static final Color WHITE = new Color(255, 255, 255);

    private List<Friend> friends = new ArrayList<>();

    private FriendManager() {
        super(new File(MeteorClient.FOLDER, "friends.nbt"));
    }

    public boolean add(Friend friend) {
        if (!friends.contains(friend)) {
            friends.add(friend);
            MeteorClient.EVENT_BUS.post(EventStore.friendListChangedEvent());
            save();
            return true;
        }

        return false;
    }

    public List<Friend> getAll() {
        return friends;
    }

    public boolean contains(Friend friend) {
        return friends.contains(friend);
    }

    public Friend get(String name) {
        for (Friend friend : friends) {
            if (friend.name.equals(name)) {
                return friend;
            }
        }

        return null;
    }

    public Friend get(PlayerEntity player) {
        return get(player.getGameProfile().getName());
    }

    public boolean isTrusted(PlayerEntity player) {
        Friend friend = get(player.getGameProfile().getName());
        return friend != null && friend.trusted;
    }

    public boolean attack(PlayerEntity player) {
        Friend friend = get(player.getGameProfile().getName());
        return friend == null || friend.attack;
    }

    public Color getColor(PlayerEntity entity, Color defaultColor) {
        Friend friend = get(entity.getGameProfile().getName());
        return friend == null ? defaultColor : friend.color;
    }

    public void addOrRemove(Friend friend) {
        if (friends.contains(friend)) remove(friend);
        else add(friend);
    }

    public boolean remove(Friend friend) {
        if (friends.remove(friend)) {
            MeteorClient.EVENT_BUS.post(EventStore.friendListChangedEvent());
            save();
            return true;
        }

        return false;
    }

    public int count() {
        return friends.size();
    }

    @Override
    public Iterator<Friend> iterator() {
        return friends.iterator();
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        ListTag friendsTag = new ListTag();
        for (Friend friend : friends) friendsTag.add(friend.toTag());
        tag.put("friends", friendsTag);

        return tag;
    }

    @Override
    public FriendManager fromTag(CompoundTag tag) {
        friends = NbtUtils.listFromTag(tag.getList("friends", 10), tag1 -> new Friend((CompoundTag) tag1));

        return this;
    }
}
