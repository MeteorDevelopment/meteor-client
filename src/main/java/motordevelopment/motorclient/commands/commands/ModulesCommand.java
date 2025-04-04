/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import motordevelopment.motorclient.commands.Command;
import motordevelopment.motorclient.systems.modules.Module;
import motordevelopment.motorclient.systems.modules.Modules;
import motordevelopment.motorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ModulesCommand extends Command {
    public ModulesCommand() {
        super("modules", "Displays a list of all modules.", "features");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            ChatUtils.info("--- Modules ((highlight)%d(default)) ---", Modules.get().getCount());

            Modules.loopCategories().forEach(category -> {
                MutableText categoryMessage = Text.literal("");
                Modules.get().getGroup(category).forEach(module -> categoryMessage.append(getModuleText(module)));
                ChatUtils.sendMsg(category.name, categoryMessage);
            });

            return SINGLE_SUCCESS;
        });
    }

    private MutableText getModuleText(Module module) {
        // Hover tooltip
        MutableText tooltip = Text.literal("");

        tooltip.append(Text.literal(module.title).formatted(Formatting.BLUE, Formatting.BOLD)).append("\n");
        tooltip.append(Text.literal(module.name).formatted(Formatting.GRAY)).append("\n\n");
        tooltip.append(Text.literal(module.description).formatted(Formatting.WHITE));

        MutableText finalModule = Text.literal(module.title);
        if (!module.isActive()) finalModule.formatted(Formatting.GRAY);
        if (!module.equals(Modules.get().getGroup(module.category).getLast())) finalModule.append(Text.literal(", ").formatted(Formatting.GRAY));
        finalModule.setStyle(finalModule.getStyle().withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip)));

        return finalModule;
    }

}
