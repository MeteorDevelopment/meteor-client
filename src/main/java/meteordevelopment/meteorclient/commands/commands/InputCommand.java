/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.datafixers.util.Pair;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.KeyBindingAccessor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.command.CommandSource;

import java.util.ArrayList;
import java.util.List;

public class InputCommand extends Command {
    private static final List<KeypressHandler> activeHandlers = new ArrayList<>();

    private static final List<Pair<KeyBinding, String>> holdKeys = List.of(
        new Pair<>(mc.options.forwardKey, "forwards"),
        new Pair<>(mc.options.backKey, "backwards"),
        new Pair<>(mc.options.leftKey, "left"),
        new Pair<>(mc.options.rightKey, "right"),
        new Pair<>(mc.options.jumpKey, "jump"),
        new Pair<>(mc.options.sneakKey, "sneak"),
        new Pair<>(mc.options.sprintKey, "sprint"),
        new Pair<>(mc.options.useKey, "use"),
        new Pair<>(mc.options.attackKey, "attack")
    );

    private static final List<Pair<KeyBinding, String>> pressKeys = List.of(
        new Pair<>(mc.options.swapHandsKey, "swap"),
        new Pair<>(mc.options.dropKey, "drop")
    );

    public InputCommand() {
        super("input", "Keyboard input simulation.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        for (Pair<KeyBinding, String> keyBinding : holdKeys) {
            builder.then(literal(keyBinding.getSecond())
                .then(argument("ticks", IntegerArgumentType.integer(1))
                    .executes(context -> {
                        activeHandlers.add(new KeypressHandler(keyBinding.getFirst(), context.getArgument("ticks", Integer.class)));
                        return SINGLE_SUCCESS;
                    })
                )
            );
        }

        for (Pair<KeyBinding, String> keyBinding : pressKeys) {
            builder.then(literal(keyBinding.getSecond())
                .executes(context -> {
                    press(keyBinding.getFirst());
                    return SINGLE_SUCCESS;
                })
            );
        }

        for (KeyBinding keyBinding : mc.options.hotbarKeys) {
            builder.then(literal(keyBinding.getTranslationKey().substring(4))
                .executes(context -> {
                    press(keyBinding);
                    return SINGLE_SUCCESS;
                })
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
                    info("(highlight)%d(default) - (highlight)%s %d(default) ticks left out of (highlight)%d(default).", i, I18n.translate(handler.key.getTranslationKey()), handler.ticks, handler.totalTicks);
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

    private static void press(KeyBinding keyBinding) {
        KeyBindingAccessor accessor = (KeyBindingAccessor) keyBinding;
        accessor.meteor$setTimesPressed(accessor.meteor$getTimesPressed() + 1);
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
