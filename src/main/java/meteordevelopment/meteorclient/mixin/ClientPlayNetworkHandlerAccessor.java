/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.message.LastSeenMessagesCollector;
import net.minecraft.network.message.MessageChain;
import net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.resource.featuretoggle.FeatureSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayNetworkHandler.class)
public interface ClientPlayNetworkHandlerAccessor {
    @Accessor("chunkLoadDistance")
    int meteor$getChunkLoadDistance();

    @Accessor("messagePacker")
    MessageChain.Packer meteor$getMessagePacker();

    @Accessor("lastSeenMessagesCollector")
    LastSeenMessagesCollector meteor$getLastSeenMessagesCollector();

    @Accessor("combinedDynamicRegistries")
    DynamicRegistryManager.Immutable meteor$getCombinedDynamicRegistries();

    @Accessor("enabledFeatures")
    FeatureSet meteor$getEnabledFeatures();

    @Accessor("COMMAND_NODE_FACTORY")
    static CommandTreeS2CPacket.NodeFactory<ClientCommandSource> meteor$getCommandNodeFactory() {
        return null;
    }
}
