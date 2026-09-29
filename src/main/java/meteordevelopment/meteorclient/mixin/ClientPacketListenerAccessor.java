/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.LastSeenMessagesTracker;
import net.minecraft.network.chat.SignedMessageChain;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPacketListener.class)
public interface ClientPacketListenerAccessor {
    @Accessor("serverChunkRadius")
    int meteor$getServerChunkRadius();

    @Accessor("signedMessageEncoder")
    SignedMessageChain.Encoder meteor$getSignedMessageEncoder();

    @Accessor("lastSeenMessages")
    LastSeenMessagesTracker meteor$getLastSeenMessages();

    @Accessor("registryAccess")
    RegistryAccess.Frozen meteor$getRegistryAccess();

    @Accessor("enabledFeatures")
    FeatureFlagSet meteor$getEnabledFeatures();

    @Accessor("COMMAND_NODE_BUILDER")
    static ClientboundCommandsPacket.NodeBuilder<ClientSuggestionProvider> meteor$getCommandNodeFactory() {
        return null;
    }
}
