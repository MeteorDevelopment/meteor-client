package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.client.network.ServerInfo;
import net.minecraft.command.CommandSource;

import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.utils.player.ChatUtils;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class Server extends Command {

    public Server() {
        super("server", "Prints server information");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if(mc.isIntegratedServerRunning()) {
                ChatUtils.info("Singleplayer");
                return SINGLE_SUCCESS;
            }
            ServerInfo server = mc.getCurrentServerEntry();
            ChatUtils.info("IP: %s", server.address);
            ChatUtils.info("Version: %s", server.version.asString());
            ChatUtils.info("Protocol Version: %d", server.protocolVersion);
            
            return SINGLE_SUCCESS;
        });

    }
    
}
