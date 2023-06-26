package anticope.rejects.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.util.GlfwUtil;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.text.Text;
import org.apache.commons.lang3.SystemUtils;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class KickCommand extends Command {

    public KickCommand() {
        super("kick", "Kick or disconnect yourself from the server", "disconnect", "quit");
    }

    private static void shutdown() throws Exception {
        String cmd;
        if (SystemUtils.IS_OS_AIX)
            cmd = "shutdown -Fh 0";
        else if (SystemUtils.IS_OS_FREE_BSD || SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_MAC_OSX || SystemUtils.IS_OS_NET_BSD || SystemUtils.IS_OS_OPEN_BSD || SystemUtils.IS_OS_UNIX)
            cmd = "shutdown -h now";
        else if (SystemUtils.IS_OS_HP_UX)
            cmd = "shutdown -hy 0";
        else if (SystemUtils.IS_OS_IRIX)
            cmd = "shutdown -y -g 0";
        else if (SystemUtils.IS_OS_SOLARIS || SystemUtils.IS_OS_SUN_OS)
            cmd = "shutdown -y -i5 -g 0";
        else if (SystemUtils.IS_OS_WINDOWS)
            cmd = "shutdown.exe /s /t 0";
        else
            throw new Exception("Unsupported operating system.");

        Runtime.getRuntime().exec(cmd);
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("disconnect").executes(ctx -> {
            mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("Disconnected via .kick command")));
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("pos").executes(ctx -> {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, !mc.player.isOnGround()));
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("hurt").executes(ctx -> {
            mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(mc.player, mc.player.isSneaking()));
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("chat").executes(ctx -> {
            ChatUtils.sendPlayerMsg("§0§1§");
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("shutdown").executes(ctx -> {
            try {
                shutdown();
            } catch (Exception exception) {
                error("Couldn't disconnect. IOException");
            }
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("crash").executes(ctx -> {
            GlfwUtil.makeJvmCrash();
            return SINGLE_SUCCESS;
        }));
    }
}
