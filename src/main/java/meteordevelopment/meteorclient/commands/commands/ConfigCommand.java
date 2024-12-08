/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.SettingArgumentType;
import meteordevelopment.meteorclient.commands.arguments.SettingValueArgumentType;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.tabs.builtin.ConfigTab;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.command.CommandSource;

public class ConfigCommand extends Command {
    public ConfigCommand() {
        super("config", "Allows you to view and change meteor configurations.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            Tabs.get().stream().filter(ConfigTab.class::isInstance).map(tab -> tab.createScreen(GuiThemes.get())).findAny().ifPresent(screen -> {
                screen.parent = null;
                Utils.screenToOpen = screen;
            });
            return SINGLE_SUCCESS;
        });

        builder.then(argument("setting", SettingArgumentType.create(ctx -> Config.get().settings))
            .executes(context -> {
                Setting<?> setting = SettingArgumentType.get(context);

                info("Setting (highlight)%s(default) is (highlight)%s(default).", setting.title, setting.get());

                return SINGLE_SUCCESS;
            })
            .then(argument("value", SettingValueArgumentType.create(SettingArgumentType::get)).executes(context -> {
                Setting<?> setting = SettingArgumentType.get(context);
                String value = SettingValueArgumentType.get(context);

                if (setting.parse(value)) {
                    info("Setting (highlight)%s(default) changed to (highlight)%s(default).", setting.title, value);
                }

                return SINGLE_SUCCESS;
            }))
        );
    }
}
