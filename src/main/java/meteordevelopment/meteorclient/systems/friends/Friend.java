/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.friends;

import com.mojang.util.UUIDTypeAdapter;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.NbtException;
import meteordevelopment.meteorclient.utils.network.Http;
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

    public boolean updateName() {
        NameResponse fetch = Http.get("https://sessionserver.mojang.com/session/minecraft/profile/" + UUIDTypeAdapter.fromUUID(id)).sendJson(NameResponse.class);
        if (fetch == null || fetch.name == null || fetch.name.isBlank()) return false;

        name = fetch.name;
        return true;
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("id", UUIDTypeAdapter.fromUUID(id));

        return tag;
    }

    @Override
    public Friend fromTag(NbtCompound tag) {
        if (!tag.contains("id")) throw new NbtException();

        id = UUIDTypeAdapter.fromString(tag.getString("id"));

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Friend friend = (Friend) o;
        return Objects.equals(id, friend.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    private static class NameResponse {
        String name;
    }
}
