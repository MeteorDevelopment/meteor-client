/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.friends;

import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

import java.util.Objects;

public class Friend implements ISerializable<Friend> {
    public String name;


    public Friend(String name) {
        this.name = name;
    }

    public Friend(PlayerEntity player) {
        this(player.getEntityName());
    }

    public Friend(NbtCompound tag) {
        fromTag(tag);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.putString("name", name);
        return tag;
    }

    @Override
    public Friend fromTag(NbtCompound tag) {
        name = tag.getString("name");
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
