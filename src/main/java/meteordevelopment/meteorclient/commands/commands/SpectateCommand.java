/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerArgumentType;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

public class SpectateCommand extends Command {

    private final StaticListener shiftListener = new StaticListener();

    public SpectateCommand() {
        super("spectate", "Allows you to spectate nearby players");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("reset").executes(context -> {
            mc.setCameraEntity(mc.player);
            return SINGLE_SUCCESS;
        }));

        builder.then(argument("player", PlayerArgumentType.create()).executes(context -> {
            mc.setCameraEntity(PlayerArgumentType.get(context));
            mc.player.sendMessage(Text.literal("Sneak to un-spectate."), true);
            MeteorClient.EVENT_BUS.subscribe(shiftListener);
            return SINGLE_SUCCESS;
        }));
    }

    private static class StaticListener {
        @EventHandler
        private void onKey(KeyEvent event) {
            if (mc.options.sneakKey.matchesKey(event.key, 0) || mc.options.sneakKey.matchesMouse(event.key)) {
                mc.setCameraEntity(mc.player);
                event.cancel();
                MeteorClient.EVENT_BUS.unsubscribe(this);
            }
        }
    }
}
