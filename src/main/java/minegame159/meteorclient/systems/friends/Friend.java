/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.friends;

import minegame159.meteorclient.utils.entity.FriendType;
import minegame159.meteorclient.utils.misc.ISerializable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;

import java.util.Objects;

public class Friend implements ISerializable<Friend> {
    public String name;
    public FriendType type = FriendType.Neutral;

    public Friend(String name) {
        this.name = name;
    }

    public Friend(PlayerEntity player) {
        this(player.getGameProfile().getName());
    }

    public Friend(CompoundTag tag) {
        fromTag(tag);
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        tag.putString("type", type.name());
        return tag;
    }

    @Override
    public Friend fromTag(CompoundTag tag) {
        name = tag.getString("name");
        if (tag.contains("type")) type = FriendType.valueOf(tag.getString("type"));
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Friend friend = (Friend) o;
        return Objects.equals(name, friend.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
