/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package org.meteordev.meteor.api.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;

/** Base interface to create a Meteor command. */
public interface Command {
    /** @return the main name for the command. */
    String getName();

    /** @return the description for the command. */
    String getDescription();

    /** @return all the different aliases for the command. */
    String[] getAliases();

    /** This method is called for the command's name and for every alias and should build the command tree. */
    void build(LiteralArgumentBuilder<CommandSource> builder);
}
