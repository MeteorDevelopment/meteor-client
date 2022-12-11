/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package org.meteordev.meteor.api.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.command.CommandSource;
import org.meteordev.meteor.api.MeteorAPI;
import org.meteordev.meteor.api.addons.Addon;

/** Default implementation of the {@link Command} API. */
public abstract class AbstractCommand implements Command {
    protected final Addon addon;
    protected final String name, description;
    protected final String[] aliases;

    public AbstractCommand(Addon addon, String name, String description, String... aliases) {
        this.addon = addon;
        this.name = name;
        this.description = description;
        this.aliases = aliases;

        MeteorAPI.getInstance().getAddons().checkValid(addon);
    }

    @Override
    public Addon getAddon() {
        return addon;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String[] getAliases() {
        return aliases;
    }

    protected static final int SUCCESS = com.mojang.brigadier.Command.SINGLE_SUCCESS;

    protected static <T> RequiredArgumentBuilder<CommandSource, T> argument(final String name, final ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    protected static LiteralArgumentBuilder<CommandSource> literal(final String name) {
        return LiteralArgumentBuilder.literal(name);
    }
}
