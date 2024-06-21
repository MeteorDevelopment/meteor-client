/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerArgumentType;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.AutoWasp;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

public class WaspCommand extends Command {
    private static final SimpleCommandExceptionType CANT_WASP_SELF = new SimpleCommandExceptionType(Text.literal("You cannot target yourself!"));

    public WaspCommand() {
        super("wasp", "Sets the auto wasp target.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        AutoWasp wasp = Modules.get().get(AutoWasp.class);

        builder.then(literal("reset").executes(context -> {
            if (wasp.isActive()) wasp.toggle();
            return SINGLE_SUCCESS;
        }));

        builder.then(argument("player", PlayerArgumentType.create()).executes(context -> {
            PlayerEntity player = PlayerArgumentType.get(context);

            if (player == mc.player) throw CANT_WASP_SELF.create();

            wasp.target = player;
            if (!wasp.isActive()) wasp.toggle();
            info(player.getName().getString() + " set as target.");
            return SINGLE_SUCCESS;
        }));
    }
}
