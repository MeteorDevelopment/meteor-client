/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
public class GcCommand extends Command {

    private static final SimpleCommandExceptionType COULDNT_CALL_GC = new SimpleCommandExceptionType(Text.literal("Couldn't call the garbage collector"));

    public GcCommand() {
        super("gc", "calls the Java garbage collector.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            try {
                System.gc();
                info("Successfully called garbage collector");
                return SINGLE_SUCCESS;
            } catch (Exception e) {
                throw COULDNT_CALL_GC.create();
            }
        });
    }
}
