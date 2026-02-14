/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.text.MessageBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ModulesCommand extends Command {
    public ModulesCommand() {
        super("modules", "features");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            this.info("--- Modules (%s) ---", MessageBuilder.highlight(Modules.get().getCount())).send();

            Modules.loopCategories().forEach(category -> {
                MutableText categoryMessage = Text.empty();
                Modules.get().getGroup(category).forEach(module -> categoryMessage.append(getModuleText(module)));
                this.info(categoryMessage).prefix(MeteorClient.translatable(category.translationKey)).send();
            });

            return SINGLE_SUCCESS;
        });
    }

    private MutableText getModuleText(Module module) {
        // Hover tooltip
        MutableText tooltip = Text.empty();

        tooltip.append(module.getTitleText().formatted(Formatting.BLUE, Formatting.BOLD)).append("\n");
        tooltip.append(Text.literal(module.name).formatted(Formatting.GRAY)).append("\n\n");
        tooltip.append(module.getDescriptionText().formatted(Formatting.WHITE));

        MutableText finalModule = module.getTitleText();
        if (!module.isActive()) finalModule.formatted(Formatting.GRAY);
        if (!module.equals(Modules.get().getGroup(module.category).getLast())) finalModule.append(Text.literal(", ").formatted(Formatting.GRAY));
        finalModule.setStyle(finalModule.getStyle().withHoverEvent(new HoverEvent.ShowText(tooltip)));

        return finalModule;
    }

}
