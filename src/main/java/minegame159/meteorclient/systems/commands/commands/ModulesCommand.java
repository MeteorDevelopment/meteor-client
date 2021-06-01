/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.systems.commands.Command;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.text.BaseText;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class ModulesCommand extends Command {
    public ModulesCommand() {
        super("modules", "Displays a list of all modules.", "features");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            ChatUtils.info("--- Modules ((highlight)%d(default)) ---", Modules.get().getCount());

            Modules.loopCategories().forEach(category -> {
                BaseText categoryMessage = new LiteralText("");
                Modules.get().getGroup(category).forEach(module -> categoryMessage.append(getModuleText(module)));
                ChatUtils.sendMsg(category.name, categoryMessage);
            });

            return SINGLE_SUCCESS;
        });
    }

    private BaseText getModuleText(Module module) {
        // Hover tooltip
        BaseText tooltip = new LiteralText("");

        tooltip.append(new LiteralText(module.title).formatted(Formatting.BLUE, Formatting.BOLD)).append("\n");
        tooltip.append(new LiteralText(module.name).formatted(Formatting.GRAY)).append("\n\n");
        tooltip.append(new LiteralText(module.description).formatted(Formatting.WHITE));

        BaseText finalModule = new LiteralText(module.title);
        if (module != Modules.get().getList().get(Modules.get().getList().size() - 1)) finalModule.append(new LiteralText(", ").formatted(Formatting.GRAY));
        finalModule.setStyle(finalModule.getStyle().withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip)));

        return finalModule;
    }

}
