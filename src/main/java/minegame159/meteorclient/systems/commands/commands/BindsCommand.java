/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.systems.commands.Command;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.stream.Collectors;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

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
                    .collect(Collectors.toList());

            ChatUtils.info("--- (highlight)%d(default) bound modules ---", modules.size());

            for (Module module : modules) {
                HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, getTooltip(module));

                MutableText text = new LiteralText(module.title).formatted(Formatting.WHITE);
                text.setStyle(text.getStyle().withHoverEvent(hoverEvent));

                MutableText sep = new LiteralText(" - ");
                sep.setStyle(sep.getStyle().withHoverEvent(hoverEvent));
                text.append(sep.formatted(Formatting.GRAY));

                MutableText key = new LiteralText(module.keybind.toString());
                key.setStyle(key.getStyle().withHoverEvent(hoverEvent));
                text.append(key.formatted(Formatting.GRAY));

                ChatUtils.info(text);
            }

            return SINGLE_SUCCESS;
        });
    }

    private MutableText getTooltip(Module module) {
        MutableText tooltip = new LiteralText(Utils.nameToTitle(module.title)).formatted(Formatting.BLUE, Formatting.BOLD).append("\n\n");
        tooltip.append(new LiteralText(module.description).formatted(Formatting.WHITE));

        return tooltip;
    }
}
