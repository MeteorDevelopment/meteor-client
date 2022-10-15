/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoReconnect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class ReconnectCommand extends Command {
    public ReconnectCommand() {
        super("reconnect", "Reconnects server.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            mc.world.disconnect();
            AutoReconnect autoReconnect = Modules.get().get(AutoReconnect.class);
            ConnectScreen.connect(new MultiplayerScreen(new TitleScreen()), MinecraftClient.getInstance(),
                ServerAddress.parse(autoReconnect.lastServerInfo.address), autoReconnect.lastServerInfo);
            return SINGLE_SUCCESS;
        });
    }
}
