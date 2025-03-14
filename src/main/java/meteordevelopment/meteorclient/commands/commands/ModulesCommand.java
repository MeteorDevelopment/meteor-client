/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.ModuleArgumentType;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class ModulesCommand extends Command {
    public ModulesCommand() {
        super("modules", "List or toggle modules.", "features");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {

        builder.then(literal("toggle").then(argument("module", ModuleArgumentType.create()).executes(context -> {
            Module module = ModuleArgumentType.get(context);
            module.toggle();
            module.sendToggledMsg();
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("enable").then(argument("module", ModuleArgumentType.create()).executes(context -> {
            Module module = ModuleArgumentType.get(context);

            if(module.isActive()) {
                ChatUtils.error("Module is already enabled.");
                return SINGLE_SUCCESS;
            }

            module.toggle();
            module.sendToggledMsg();
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("disable").then(argument("module", ModuleArgumentType.create()).executes(context -> {
            Module module = ModuleArgumentType.get(context);

            if(!module.isActive()) {
                ChatUtils.error("Module is already disabled.");
                return SINGLE_SUCCESS;
            }

            module.toggle();
            module.sendToggledMsg();
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("list").executes(context -> {
            ChatUtils.info("--- Modules ((highlight)%d(default)) ---", Modules.get().getCount());

            Modules.loopCategories().forEach(category -> {
                MutableText categoryMessage = Text.literal("");
                Modules.get().getGroup(category).forEach(module -> categoryMessage.append(getModuleText(module)));
                ChatUtils.sendMsg(category.name, categoryMessage);
            });

            return SINGLE_SUCCESS;
        }));
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
