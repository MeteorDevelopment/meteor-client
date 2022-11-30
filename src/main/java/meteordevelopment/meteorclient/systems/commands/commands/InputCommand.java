package meteordevelopment.meteorclient.systems.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.command.CommandSource;

import java.util.HashMap;
import java.util.Map;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class InputCommand extends Command {
    private static final Map<KeyBinding, String> validKeys = new HashMap<>();
    static {
        validKeys.put(mc.options.forwardKey, "forwards");
        validKeys.put(mc.options.backKey, "backwards");
        validKeys.put(mc.options.leftKey, "left");
        validKeys.put(mc.options.rightKey, "right");
        validKeys.put(mc.options.jumpKey, "jump");
        validKeys.put(mc.options.sneakKey, "sneak");
        validKeys.put(mc.options.useKey, "use");
        validKeys.put(mc.options.attackKey, "attack");
    }

    public InputCommand() {
        super("input", "Keyboard input simulation.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        for (KeyBinding keyBinding : validKeys.keySet()) {
            builder.then(literal(validKeys.get(keyBinding))
                .then(argument("ticks", IntegerArgumentType.integer(1))
                    .executes(context -> {
                        new KeypressHandler(keyBinding, context.getArgument("ticks", Integer.class));
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

            ChatUtils.info("%s pressed: %b".formatted(validKeys.get(key), key.isPressed()));
        }
    }
}
