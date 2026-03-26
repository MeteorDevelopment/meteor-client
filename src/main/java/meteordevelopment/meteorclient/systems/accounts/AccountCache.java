/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.accounts;

import com.mojang.util.UndashedUuid;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.NbtException;
import meteordevelopment.meteorclient.utils.render.PlayerHeadTexture;
import meteordevelopment.meteorclient.utils.render.PlayerHeadUtils;
import net.minecraft.nbt.CompoundTag;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class AccountCache implements ISerializable<AccountCache> {
    public String username = "";
    public String uuid = "";
    private PlayerHeadTexture headTexture;

    public PlayerHeadTexture getHeadTexture() {
        return headTexture != null ? headTexture : PlayerHeadUtils.STEVE_HEAD;
    }

    public void loadHead() {
        if (uuid == null || uuid.isBlank()) return;
        mc.execute(() -> headTexture = PlayerHeadUtils.fetchHead(UndashedUuid.fromStringLenient(uuid)));
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putString("username", username);
        tag.putString("uuid", uuid);

        return tag;
    }

    @Override
    public AccountCache fromTag(CompoundTag tag) {
        if (tag.getString("username").isEmpty() || tag.getString("uuid").isEmpty()) throw new NbtException();

        username = tag.getString("username").get();
        uuid = tag.getString("uuid").get();
        loadHead();

        return this;
    }
}
