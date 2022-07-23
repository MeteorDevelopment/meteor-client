/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.encryption.PlayerPublicKey;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

public class PlayerListEntryFactory extends PlayerListS2CPacket {
    private static final PlayerListEntryFactory INSTANCE = new PlayerListEntryFactory();

    public PlayerListEntryFactory() {
        super(null, new ServerPlayerEntity[0]);
    }

    public static Entry create(GameProfile profile, int latency, GameMode gameMode, Text displayName, PlayerPublicKey.PublicKeyData publicKeyData) {
        return INSTANCE._create(profile, latency, gameMode, displayName, publicKeyData);
    }

    private Entry _create(GameProfile profile, int latency, GameMode gameMode, Text displayName, PlayerPublicKey.PublicKeyData publicKeyData) {
        return new Entry(profile, latency, gameMode, displayName, publicKeyData);
    }
}
