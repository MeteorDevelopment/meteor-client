/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.systems.commands.Command;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.command.CommandSource;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class ServerCommand extends Command {

    public ServerCommand() {
        super("server", "Prints server information");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if(mc.isIntegratedServerRunning()) {
                IntegratedServer server = mc.getServer();
                ChatUtils.prefixInfo("Server","Singleplayer");
                if (server != null) {
                    ChatUtils.prefixInfo("Server", "Version: %s", server.getVersion());
                }
                return SINGLE_SUCCESS;
            }
            ServerInfo server = mc.getCurrentServerEntry();

            if (server == null) {
                ChatUtils.prefixError("Server","Couldn't obtain any server information.");
                return SINGLE_SUCCESS;
            }

            ChatUtils.prefixInfo("Server","IP: %s", server.address);
            String serverType = mc.player.getServerBrand();
            if (serverType == null) {
                serverType = "unknown";
            }
            ChatUtils.prefixInfo("Server","Type: %s", serverType);

            BaseText motd = new LiteralText("Motd: ");
            if (server.label != null) {
                motd.append(server.label);
            } else {
                motd.append(new LiteralText("unknown"));
            }

            ChatUtils.info("Server", motd);
            
            BaseText version = new LiteralText("Version: ");
            version.append(server.version);
            ChatUtils.info("Server", version);
            
            ChatUtils.prefixInfo("Server","Protocol version: %d", server.protocolVersion);
            
            return SINGLE_SUCCESS;
        });
    }
    
}
