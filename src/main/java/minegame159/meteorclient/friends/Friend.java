/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.friends;

import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.ISerializable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;

import java.util.Objects;

public class Friend implements ISerializable<Friend> {
    public String name;
    public boolean trusted = true;
    public Color color = new Color(0, 255, 180);
    public boolean attack = false;
    public boolean showInTracers = true;

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
        tag.putBoolean("trusted", trusted);
        tag.put("color", color.toTag());
        tag.putBoolean("attack", attack);
        tag.putBoolean("showInTracers", showInTracers);

        return tag;
    }

    @Override
    public Friend fromTag(CompoundTag tag) {
        name = tag.getString("name");
        trusted = tag.getBoolean("trusted");
        color.fromTag(tag.getCompound("color"));
        attack = tag.getBoolean("attack");
        showInTracers = tag.getBoolean("showInTracers");

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
