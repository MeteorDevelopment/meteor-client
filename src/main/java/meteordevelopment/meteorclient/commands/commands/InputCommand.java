/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.command.CommandSource;

import java.util.Map;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class InputCommand extends Command {
    private static final Map<KeyBinding, String> keys = Map.of(
        mc.options.forwardKey, "forwards",
        mc.options.backKey, "backwards",
        mc.options.leftKey, "left",
        mc.options.rightKey, "right",
        mc.options.jumpKey, "jump",
        mc.options.sneakKey, "sneak",
        mc.options.useKey, "use",
        mc.options.attackKey, "attack"
    );

    public InputCommand() {
        super("input", "Keyboard input simulation.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        for (Map.Entry<KeyBinding, String> keyBinding : keys.entrySet()) {
            builder.then(literal(keyBinding.getValue())
                .then(argument("ticks", IntegerArgumentType.integer(1))
                    .executes(context -> {
                        new KeypressHandler(keyBinding.getKey(), context.getArgument("ticks", Integer.class));
                        return SINGLE_SUCCESS;
                    })
                )
            );
        }
    }

    private static class KeypressHandler {
        private final KeyBinding key;
        private int ticks;

        public KeypressHandler(KeyBinding key, int ticks) {
            this.key = key;
            this.ticks = ticks;

            MeteorClient.EVENT_BUS.subscribe(this);
        }

        @EventHandler
        private void onTick(TickEvent.Post event) {
            if (ticks-- > 0) key.setPressed(true);
            else {
                key.setPressed(false);
                MeteorClient.EVENT_BUS.unsubscribe(this);
            }
        }
    }
}
