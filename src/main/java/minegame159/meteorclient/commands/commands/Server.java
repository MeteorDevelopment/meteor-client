package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.client.network.ServerInfo;
import net.minecraft.command.CommandSource;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;

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

            ChatUtils.prefixInfo("Server","IP: %s", server.address);
            ChatUtils.prefixInfo("Server","Type: %s", mc.player.getServerBrand());

            BaseText motd = new LiteralText("Motd: ");
            motd.append(server.label);
            ChatUtils.info("Server", motd);
            
            BaseText version = new LiteralText("Version: ");
            version.append(server.version);
            ChatUtils.info("Server",version);
            
            ChatUtils.prefixInfo("Server","Protocol version: %d", server.protocolVersion);
            
            return SINGLE_SUCCESS;
        });
    }
    
}
