/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.ModuleArgumentType;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.command.CommandSource;

import java.util.Set;

public class SettingCommand extends Command {
    public SettingCommand() {
        super("settings", "Allows you to view and change module settings.", "s");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(
            literal("hud")
                .executes(context -> {
                    TabScreen screen = Tabs.get().get(3).createScreen(GuiThemes.get());
                    screen.parent = null;

                    Utils.screenToOpen = screen;
                    return SINGLE_SUCCESS;
                })
        );

        // Open module screen
        builder.then(
            argument("module", ModuleArgumentType.create())
                .executes(context -> {
                    Module module = context.getArgument("module", Module.class);

                    WidgetScreen screen = GuiThemes.get().moduleScreen(module);
                    screen.parent = null;

                    Utils.screenToOpen = screen;
                    return SINGLE_SUCCESS;
                })
        );

        // View or change settings
        for (Module module : Modules.get().getAll()) {
            LiteralArgumentBuilder<CommandSource> moduleBuilder = literal(module.name);
            builder.then(moduleBuilder);

            if (hasDuplicateSettingNames(module.settings)) {
                for (SettingGroup settingGroup : module.settings) {
                    LiteralArgumentBuilder<CommandSource> settingGroupBuilder = literal(Utils.titleToName(settingGroup.name));
                    moduleBuilder.then(settingGroupBuilder);

                    for (Setting<?> setting : settingGroup) {
                        buildSetting(settingGroupBuilder, setting, module);
                    }
                }
            } else {
                for (SettingGroup settingGroup : module.settings) {
                    for (Setting<?> setting : settingGroup) {
                        buildSetting(moduleBuilder, setting, module);
                    }
                }
            }
        }
    }

    private <T> void buildSetting(LiteralArgumentBuilder<CommandSource> parentBuilder, Setting<T> setting, Module module) {
        LiteralArgumentBuilder<CommandSource> builder = literal(setting.name);
        parentBuilder.then(builder);

        builder.then(literal("reset").executes(context -> {
            setting.reset();
            module.info("Setting (highlight)%s(default) reset.", setting.title);
            return SINGLE_SUCCESS;
        }));

        LiteralArgumentBuilder<CommandSource> getter = setting.buildGetterNode(module::info);
        builder.then(getter);
        builder.redirect(getter.build());

        setting.buildCommandNode(builder, module::info);
    }

    private static boolean hasDuplicateSettingNames(Settings settings) {
        int groups = settings.sizeGroups();
        if (groups <= 1) {
            return false;
        }

        Set<String> settingNames = new ObjectOpenHashSet<>();

        for (int i = 0; i < groups - 1; i++) {
            SettingGroup group = settings.groups.get(i);
            for (Setting<?> setting : group) {
                if (!settingNames.add(setting.name)) {
                    return true;
                }
            }
        }

        for (Setting<?> setting : settings.groups.getLast()) {
            if (settingNames.contains(setting.name)) {
                return true;
            }
        }

        return false;
    }
}
