package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.skyblock.TerminalSimulator;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public class TermsimCommand extends Command {
    public TermsimCommand() {
        super("termsim", "Opens the terminal simulator.", "ts");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(context -> {
            TerminalSimulator sim = Modules.get().get(TerminalSimulator.class);
            if (sim != null) sim.toggle();
            return SINGLE_SUCCESS;
        });
    }
}
