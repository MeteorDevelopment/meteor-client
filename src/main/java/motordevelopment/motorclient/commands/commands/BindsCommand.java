/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import motordevelopment.motorclient.commands.Command;
import motordevelopment.motorclient.systems.modules.Module;
import motordevelopment.motorclient.systems.modules.Modules;
import motordevelopment.motorclient.utils.Utils;
import motordevelopment.motorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class BindsCommand extends Command {
    public BindsCommand() {
        super("binds", "List of all bound modules.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            // Modules
            List<Module> modules = Modules.get().getAll().stream()
                .filter(module -> module.keybind.isSet())
                .toList();

            ChatUtils.info("--- Bound Modules ((highlight)%d(default)) ---", modules.size());

            for (Module module : modules) {
                HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, getTooltip(module));

                MutableText text = Text.literal(module.title).formatted(Formatting.WHITE);
                text.setStyle(text.getStyle().withHoverEvent(hoverEvent));

                MutableText sep = Text.literal(" - ");
                sep.setStyle(sep.getStyle().withHoverEvent(hoverEvent));
                text.append(sep.formatted(Formatting.GRAY));

                MutableText key = Text.literal(module.keybind.toString());
                key.setStyle(key.getStyle().withHoverEvent(hoverEvent));
                text.append(key.formatted(Formatting.GRAY));

                ChatUtils.sendMsg(text);
            }

            return SINGLE_SUCCESS;
        });
    }

    private MutableText getTooltip(Module module) {
        MutableText tooltip = Text.literal(Utils.nameToTitle(module.title)).formatted(Formatting.BLUE, Formatting.BOLD).append("\n\n");
        tooltip.append(Text.literal(module.description).formatted(Formatting.WHITE));
        return tooltip;
    }
}
