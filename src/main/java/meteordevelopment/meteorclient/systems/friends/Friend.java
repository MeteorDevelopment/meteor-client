/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.friends;

import com.mojang.util.UUIDTypeAdapter;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.render.PlayerHeadTexture;
import meteordevelopment.meteorclient.utils.render.PlayerHeadUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public class Friend implements ISerializable<Friend> {
    public String name;
    public @Nullable UUID id;
    public PlayerHeadTexture headTexture;

    public Friend(String name, @Nullable UUID id) {
        this.name = name;
        this.id = id;
        updateHead();
    }

    public Friend(PlayerEntity player) {
        this(player.getEntityName(), player.getUuid());
    }
    public Friend(String name) {
        this(name, null);
    }
    public Friend(NbtElement tag) {
        fromTag((NbtCompound) tag);
    }

    public boolean updateName() {
        if (id == null) return false;

        APIResponse res = Http.get("https://api.mojang.com/user/profile/" + UUIDTypeAdapter.fromUUID(id)).sendJson(APIResponse.class);
        if (res == null || res.name == null || res.name.isBlank()) return false;
        name = res.name;

        return true;
    }

    public void updateID() {
        if (name == null) return;

        MeteorExecutor.execute(() -> {
            APIResponse res = Http.get("https://api.mojang.com/users/profiles/minecraft/" + name).sendJson(APIResponse.class);
            if (res == null || res.id == null || res.id.isBlank()) return;
            id = UUIDTypeAdapter.fromString(res.id);
        });
    }

    public void updateHead() {
        headTexture = PlayerHeadUtils.fetchHead(name);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("name", name);
        if (id != null) tag.putString("id", UUIDTypeAdapter.fromUUID(id));

        return tag;
    }

    @Override
    public Friend fromTag(NbtCompound tag) {
        if (tag.contains("name")) {
            name = tag.getString("name");
        }

        if (tag.contains("id")) {
            id = UUIDTypeAdapter.fromString(tag.getString("id"));
        }

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

    private static class APIResponse {
        String name, id;
    }
}
