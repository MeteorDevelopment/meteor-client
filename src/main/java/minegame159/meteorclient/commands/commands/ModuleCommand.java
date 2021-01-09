/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.Config;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.commands.arguments.ModuleArgumentType;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.utils.player.Chat;
import net.minecraft.command.CommandSource;

import java.util.List;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class ModuleCommand extends Command {
    public ModuleCommand() {
        super("module", "Lists all modules.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("list").executes(context -> {
                    Chat.info("All (highlight)%d (default)modules:", ModuleManager.INSTANCE.getAll().size());
                    for (Category category : ModuleManager.CATEGORIES) {
                        List<Module> group = ModuleManager.INSTANCE.getGroup(category);
                        Chat.info("- (highlight)%s (default)(%d):", category.toString(), group.size());

                        for (Module module : group) Chat.info("  - (highlight)%s%s (default)- %s", Config.INSTANCE.getPrefix(), module.name, module.description);
                    }
                    return SINGLE_SUCCESS;
                })).then(literal("bind").then(argument("module", ModuleArgumentType.module()).executes(context -> {
                    Module m = context.getArgument("module", Module.class);

                    Chat.info("Press a key you want this module to be bound to.");
                    ModuleManager.INSTANCE.setModuleToBind(m);

                    return SINGLE_SUCCESS;
                }))).then(literal("toggle")
                        .then(argument("module", ModuleArgumentType.module())
                                .executes(context -> {
                                    Module m = context.getArgument("module", Module.class);
                                    m.toggle();
                                    m.sendToggledMsg();
                                    return SINGLE_SUCCESS;
                                }).then(literal("on").executes(context -> {
                                    Module m = context.getArgument("module", Module.class);
                                    if (!m.isActive()) m.toggle(); m.sendToggledMsg();
                                    return SINGLE_SUCCESS;
                                })).then(literal("off").executes(context -> {
                                    Module m = context.getArgument("module", Module.class);
                                    if (m.isActive()) m.toggle(); m.sendToggledMsg();
                                    return SINGLE_SUCCESS;
                                }))
                        ));
    }
}
