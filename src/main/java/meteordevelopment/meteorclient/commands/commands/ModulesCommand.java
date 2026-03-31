/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class ModulesCommand extends Command {
    public ModulesCommand() {
        super("modules", "Displays a list of all modules.", "features");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.executes(context -> {
            ChatUtils.info("--- Modules ((highlight)%d(default)) ---", Modules.get().getCount());

            Modules.loopCategories().forEach(category -> {
                MutableComponent categoryMessage = MutableComponent.literal("");
                Modules.get().getGroup(category).forEach(module -> categoryMessage.append(getModuleText(module)));
                ChatUtils.sendMsg(category.name, categoryMessage);
            });

            return SINGLE_SUCCESS;
        });
    }

    private MutableComponent getModuleText(Module module) {
        // Hover tooltip
        MutableComponent tooltip = MutableComponent.literal("");

        tooltip.append(MutableComponent.literal(module.title).formatted(ChatFormatting.BLUE, ChatFormatting.BOLD)).append("\n");
        tooltip.append(MutableComponent.literal(module.name).formatted(ChatFormatting.GRAY)).append("\n\n");
        tooltip.append(MutableComponent.literal(module.description).formatted(ChatFormatting.WHITE));

        MutableComponent finalModule = MutableComponent.literal(module.title);
        if (!module.isActive()) finalModule.formatted(ChatFormatting.GRAY);
        if (!module.equals(Modules.get().getGroup(module.category).getLast()))
            finalModule.append(MutableComponent.literal(", ").formatted(ChatFormatting.GRAY));
        finalModule.setStyle(finalModule.getStyle().withHoverEvent(new HoverEvent.ShowText(tooltip)));

        return finalModule;
    }

}
