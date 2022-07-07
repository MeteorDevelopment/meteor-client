/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.friends;

import com.mojang.util.UUIDTypeAdapter;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.NbtException;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

import java.util.Objects;
import java.util.UUID;

public class Friend implements ISerializable<Friend> {
    public String name;
    public UUID id;

    public Friend(String name, UUID id) {
        this.name = name;
        this.id = id;
    }

    public Friend(PlayerEntity player) {
        this(player.getEntityName(), player.getUuid());
    }

    public Friend(PlayerListEntry entry) {
        this(entry.getProfile().getName(), entry.getProfile().getId());
    }

    public Friend(NbtCompound tag) {
        fromTag(tag);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.putString("name", name);
        tag.putString("id", UUIDTypeAdapter.fromUUID(id));
        return tag;
    }

    @Override
    public Friend fromTag(NbtCompound tag) {
        if (!tag.contains("id") || !tag.contains("name")) throw new NbtException();

        id = UUIDTypeAdapter.fromString(tag.getString("id"));
        name = tag.getString("name");

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Friend friend = (Friend) o;
        return Objects.equals(id, friend.id) && Objects.equals(name, friend.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
