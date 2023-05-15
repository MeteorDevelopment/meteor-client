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
import meteordevelopment.meteorclient.utils.misc.Pool;
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

    private static final Pool<KeypressHandler> keypressHandlerPool = new Pool<>(KeypressHandler::new);

    public InputCommand() {
        super("input", "Keyboard input simulation.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        for (Map.Entry<KeyBinding, String> keyBinding : keys.entrySet()) {
            builder.then(literal(keyBinding.getValue())
                .then(argument("ticks", IntegerArgumentType.integer(1))
                    .executes(context -> {
                        KeypressHandler handler = keypressHandlerPool.get();
                        handler.key = keyBinding.getKey();
                        handler.ticks = IntegerArgumentType.getInteger(context, "ticks");
                        MeteorClient.EVENT_BUS.subscribe(handler);
                        return SINGLE_SUCCESS;
                    })
                )
            );
        }
    }

    private static class KeypressHandler {
        private KeyBinding key;
        private int ticks;

        @EventHandler
        private void onTick(TickEvent.Post event) {
            if (ticks-- > 0) key.setPressed(true);
            else {
                key.setPressed(false);
                MeteorClient.EVENT_BUS.unsubscribe(this);
                keypressHandlerPool.free(this);
            }
        }
    }
}
