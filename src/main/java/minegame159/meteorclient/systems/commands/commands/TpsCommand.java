package minegame159.meteorclient.systems.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.systems.commands.Command;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import minegame159.meteorclient.utils.world.TickRate;
import net.minecraft.util.Formatting;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class TpsCommand extends Command {
    public TpsCommand() {
        super("tps", "Display current TPS");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            float tps = TickRate.INSTANCE.getTickRate();
            Formatting color = Formatting.WHITE;
            if (tps>17.0f) {
                color = Formatting.GREEN;
            }
            else if (tps>12.0f) {
                color = Formatting.YELLOW;
            }
            else {
                color = Formatting.RED;
            }
            ChatUtils.prefixInfo("TPS", "Current TPS: %s%.2f(default).", color, tps);
            return SINGLE_SUCCESS;
        });
    }
}
