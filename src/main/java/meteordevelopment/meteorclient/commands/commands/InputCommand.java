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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InputCommand extends Command {
    private static final List<KeypressHandler> activeHandlers = new ArrayList<>();

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
                        activeHandlers.add(new KeypressHandler(keyBinding.getKey(), context.getArgument("ticks", Integer.class)));
                        return SINGLE_SUCCESS;
                    })
                )
            );
        }

        builder.then(literal("clear").executes(ctx -> {
            if (activeHandlers.isEmpty()) warning("No active keypress handlers.");
            else {
                info("Cleared all keypress handlers.");
                activeHandlers.forEach(MeteorClient.EVENT_BUS::unsubscribe);
                activeHandlers.clear();
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("list").executes(ctx -> {
            if (activeHandlers.isEmpty()) warning("No active keypress handlers.");
            else {
                info("Active keypress handlers: ");
                for (int i = 0; i < activeHandlers.size(); i++) {
                    KeypressHandler handler = activeHandlers.get(i);
                    info("(highlight)%d(default) - (highlight)%s %d(default) ticks left out of (highlight)%d(default).", i, keys.get(handler.key), handler.ticks, handler.totalTicks);
                }
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("remove").then(argument("index", IntegerArgumentType.integer(0)).executes(ctx -> {
            int index = IntegerArgumentType.getInteger(ctx, "index");
            if (index >= activeHandlers.size()) warning("Index out of range.");
            else {
                info("Removed keypress handler.");
                MeteorClient.EVENT_BUS.unsubscribe(activeHandlers.get(index));
                activeHandlers.remove(index);
            }
            return SINGLE_SUCCESS;
        })));
    }

    private static class KeypressHandler {
        private final KeyBinding key;
        private final int totalTicks;
        private int ticks;

        public KeypressHandler(KeyBinding key, int ticks) {
            this.key = key;
            this.totalTicks = ticks;
            this.ticks = ticks;

            MeteorClient.EVENT_BUS.subscribe(this);
        }

        @EventHandler
        private void onTick(TickEvent.Post event) {
            if (ticks-- > 0) key.setPressed(true);
            else {
                key.setPressed(false);
                MeteorClient.EVENT_BUS.unsubscribe(this);
                activeHandlers.remove(this);
            }
        }
    }
}
