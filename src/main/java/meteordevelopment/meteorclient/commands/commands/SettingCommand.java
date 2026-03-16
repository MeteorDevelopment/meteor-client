/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.ModuleArgumentType;
import meteordevelopment.meteorclient.commands.arguments.SettingArgumentType;
import meteordevelopment.meteorclient.commands.arguments.SettingValueArgumentType;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.tabs.builtin.HudTab;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.text.MessageBuilder;
import net.minecraft.command.CommandSource;
import org.jetbrains.annotations.Nullable;

public class SettingCommand extends Command {
    public SettingCommand() {
        super("settings", "s");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(
            literal("hud")
                .executes(context -> {
                    TabScreen screen = Tabs.get(HudTab.class).createScreen(GuiThemes.get());
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
        builder.then(
                argument("module", ModuleArgumentType.create())
                .then(
                        argument("setting", SettingArgumentType.create())
                        .executes(context -> {
                            // Get setting value
                            Setting<?> setting = SettingArgumentType.get(context);
                            Module module = ModuleArgumentType.get(context);
                            Settings settings = module.settings;
                            SettingGroup settingGroup = findSettingGroup(settings, setting);

                            if (settingGroup == null) {
                                throw new IllegalStateException(String.format("Cannot find setting '%s' in module '%s'.", setting.name, module.name));
                            }

                            String key = module.settings.baseTranslationKey + "." + settingGroup.translationKey + "." + setting.name;

                            this.info("command.settings.info.get", MessageBuilder.highlight(MeteorClient.translatable(key)), MessageBuilder.highlight(setting.get()))
                                    .prefix(module).setSource(module).send();

                            return SINGLE_SUCCESS;
                        })
                        .then(
                                argument("value", SettingValueArgumentType.create())
                                .executes(context -> {
                                    // Set setting value
                                    Setting<?> setting = SettingArgumentType.get(context);
                                    String value = SettingValueArgumentType.get(context);

                                    if (setting.parse(value)) {
                                        Module module = ModuleArgumentType.get(context);
                                        Settings settings = module.settings;
                                        SettingGroup settingGroup = findSettingGroup(settings, setting);

                                        if (settingGroup == null) {
                                            throw new IllegalStateException(String.format("Cannot find setting '%s' in module '%s'.", setting.name, module.name));
                                        }

                                        String key = module.settings.baseTranslationKey + "." + settingGroup.translationKey + "." + setting.name;

                                        this.info("command.settings.info.set", MessageBuilder.highlight(MeteorClient.translatable(key)), MessageBuilder.highlight(setting.get()))
                                            .prefix(module).setSource(module).send();
                                    }

                                    return SINGLE_SUCCESS;
                                })
                        )
                )
        );
    }

    private static @Nullable SettingGroup findSettingGroup(Settings settings, Setting<?> setting) {
        for (SettingGroup sg : settings) {
            for (Setting<?> aSetting : sg) {
                if (aSetting == setting) {
                    return sg;
                }
            }
        }

        return null;
    }
}
