/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.systems.accounts;

import com.mojang.util.UndashedUuid;
import motordevelopment.motorclient.utils.misc.ISerializable;
import motordevelopment.motorclient.utils.misc.NbtException;
import motordevelopment.motorclient.utils.render.PlayerHeadTexture;
import motordevelopment.motorclient.utils.render.PlayerHeadUtils;
import net.minecraft.nbt.NbtCompound;

public class AccountCache implements ISerializable<AccountCache> {
    public String username = "";
    public String uuid = "";
    private PlayerHeadTexture headTexture;

    public PlayerHeadTexture getHeadTexture() {
        return headTexture != null ? headTexture : PlayerHeadUtils.STEVE_HEAD;
    }

    public void loadHead() {
        if (uuid == null || uuid.isBlank()) return;
        headTexture = PlayerHeadUtils.fetchHead(UndashedUuid.fromStringLenient(uuid));
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("username", username);
        tag.putString("uuid", uuid);

        return tag;
    }

    @Override
    public AccountCache fromTag(NbtCompound tag) {
        if (!tag.contains("username") || !tag.contains("uuid")) throw new NbtException();

        username = tag.getString("username");
        uuid = tag.getString("uuid");
        loadHead();

        return this;
    }
}
