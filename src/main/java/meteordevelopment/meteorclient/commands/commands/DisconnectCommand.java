/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class DisconnectCommand extends Command {
    public DisconnectCommand() {
        super("disconnect", "Disconnect from the server", "dc");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("%s[%sDisconnectCommand%s] Disconnected by user.".formatted(Formatting.GRAY, Formatting.BLUE, Formatting.GRAY))));
            return SINGLE_SUCCESS;
        });

        builder.then(argument("reason", StringArgumentType.greedyString()).executes(context -> {
            mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal(StringArgumentType.getString(context, "reason"))));
            return SINGLE_SUCCESS;
        }));
    }
}
