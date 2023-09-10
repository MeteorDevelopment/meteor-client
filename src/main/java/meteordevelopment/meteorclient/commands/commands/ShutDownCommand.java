/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ShutDownCommand extends Command {
    public ShutDownCommand() {
        super("shutdown", "Closes the client.", "shut");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            info("Shutting down...");
            this.disconnect();

            mc.stop();

            return SINGLE_SUCCESS;
        });
    }

    /**
     * Repurposed from GameMenuScreen
     */
    private void disconnect() {
        boolean isSp = mc.isInSingleplayer();

        if (mc.world != null) mc.world.disconnect();

        // Handle network
        if (isSp) {
            mc.disconnect(new MessageScreen(Text.translatable("menu.savingLevel")));
        } else {
            mc.disconnect();
        }
    }
}
