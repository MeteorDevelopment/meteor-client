/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.mixin.ClientPacketListenerAccessor;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.LastSeenMessagesTracker;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.util.Crypt;
import org.meteordev.starscript.Script;

import java.time.Instant;

public class SayCommand extends Command {
    public SayCommand() {
        super("say", "Sends messages in chat.");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(argument("message", StringArgumentType.greedyString()).executes(context -> {
            String msg = context.getArgument("message", String.class);
            Script script = MeteorStarscript.compile(msg);

            if (script != null) {
                String message = MeteorStarscript.run(script);

                if (message != null) {
                    Instant instant = Instant.now();
                    long l = Crypt.SaltSupplier.getLong();
                    ClientPacketListener handler = mc.getConnection();
                    LastSeenMessagesTracker.Update lastSeenMessages = ((ClientPacketListenerAccessor) handler).meteor$getLastSeenMessages().generateAndApplyUpdate();
                    MessageSignature messageSignatureData = ((ClientPacketListenerAccessor) handler).meteor$getSignedMessageEncoder().pack(new SignedMessageBody(message, instant, l, lastSeenMessages.lastSeen()));
                    handler.send(new ServerboundChatPacket(message, instant, l, messageSignatureData, lastSeenMessages.update()));
                }
            }

            return SINGLE_SUCCESS;
        }));
    }
}
