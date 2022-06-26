/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.friends;

import com.mojang.util.UUIDTypeAdapter;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
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
        this(player.getGameProfile().getName(), player.getUuid());
    }

    public Friend(PlayerListEntry entry) {
        this(entry.getProfile().getName(), entry.getProfile().getId());
    }

    public Friend(NbtCompound tag) {
        fromTag(tag);
    }

    public void refresh() {
        name = getName(id);
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
        if (tag.contains("id") && isUUIDValid(tag.getString("id"))) {
            id = UUIDTypeAdapter.fromString(tag.getString("id"));
            name = getName(id);
        } else if (tag.contains("name")) {
            return Friends.get().getFromName(tag.getString("name"));
        }

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

    private boolean isUUIDValid(String id) {
        try {
            UUIDTypeAdapter.fromString(id);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String getName(UUID uuid) {
        return ((NameResponse) Http.get("https://sessionserver.mojang.com/session/minecraft/profile/" + UUIDTypeAdapter.fromUUID(uuid)).sendJson(NameResponse.class)).name;
    }

    private static class NameResponse {
        String name;
    }
}
