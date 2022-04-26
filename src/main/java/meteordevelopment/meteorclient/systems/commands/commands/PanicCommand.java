package meteordevelopment.meteorclient.systems.commands.commands;

import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import net.minecraft.command.CommandSource;
import meteordevelopment.meteorclient.systems.hud.HUD;
import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

import java.util.Collection;
import java.util.Iterator;

public class PanicCommand extends Command {
    public PanicCommand() {
        super("panic", "Turns all modules off.", "p");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
                Modules.get().disableAll();
                HUD.get().active = false;

                return SINGLE_SUCCESS;
        });
    }
}
