/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.encryption.PublicPlayerSession;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class PlayerListEntryFactory extends PlayerListS2CPacket {
    private static final PlayerListEntryFactory INSTANCE = new PlayerListEntryFactory();

    public PlayerListEntryFactory() {
        super(null, List.of(new ServerPlayerEntity[0]));
    }

    public static Entry create(GameProfile profile, int latency, GameMode gameMode, Text displayName, @Nullable PublicPlayerSession.Serialized serialized) {
        return INSTANCE._create(profile.getId(), profile, false, latency, gameMode, displayName, serialized);
    }

    private Entry _create(UUID uUID, GameProfile gameProfile, boolean bl, int i, GameMode gameMode, @Nullable Text text, @Nullable PublicPlayerSession.Serialized serialized) {
        return new Entry(uUID, gameProfile, bl, i, gameMode, text, serialized);
    }
}
